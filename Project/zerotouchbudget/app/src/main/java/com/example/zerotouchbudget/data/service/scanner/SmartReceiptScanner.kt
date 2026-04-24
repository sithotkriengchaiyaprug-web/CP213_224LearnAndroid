package com.example.zerotouchbudget.data.service.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.zerotouchbudget.data.local.dao.ProcessedReceiptDao
import com.example.zerotouchbudget.data.local.entity.ProcessedReceiptEntity
import com.example.zerotouchbudget.domain.model.Transaction
import com.example.zerotouchbudget.domain.usecase.ProcessReceiptImageUseCase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

data class ImageMetadata(
    val uri: String,
    val name: String,
    val relativePath: String,
    val dateAdded: Long,
    val size: Long
)

@Singleton
class SmartReceiptScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiUseCase: ProcessReceiptImageUseCase,
    private val receiptDao: ProcessedReceiptDao
) {
    private val TAG = "SmartReceiptScanner"
    // ML Kit recognizer
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    // Concurrency Control: ป้องกัน OOM และ CPU ร้อน
    private val scanDispatcher = Dispatchers.IO.limitedParallelism(2)

    suspend fun scan(limit: Int = 100, sinceTimestamp: Long = 0) = withContext(scanDispatcher) {
        Log.d(TAG, "Starting scan limit=$limit since=$sinceTimestamp")
        val images = queryMediaStore(limit, sinceTimestamp)
        
        images.map { image ->
            async {
                // Deduplication Check
                if (receiptDao.isAlreadyProcessed(image.uri)) return@async

                // Step 2 & 3: Filter & Score (No Decode)
                val score = calculateScore(image)
                if (score < 3) {
                    Log.d(TAG, "Skipping ${image.name} (score: $score)")
                    return@async
                }

                Log.d(TAG, "Processing ${image.name} (score: $score)")
                
                // 2. Bitmap Optimization: โหลดรูปแบบย่อส่วน ไม่ให้เกิน 1024px กัน OOM
                val bitmap = loadOptimizedBitmap(Uri.parse(image.uri), maxWidth = 1024, maxHeight = 1024)
                if (bitmap == null) {
                    Log.e(TAG, "Failed to load bitmap for ${image.uri}")
                    return@async
                }
                
                // 3. OCR Cancellable: ใช้ suspendCancellableCoroutine
                val hasSlipKeywords = performOcrPreCheck(bitmap)
                if (!hasSlipKeywords) {
                    Log.d(TAG, "Failed OCR pre-check for ${image.name}")
                    receiptDao.insert(ProcessedReceiptEntity(image.uri, image.dateAdded, image.size))
                    return@async
                }

                Log.d(TAG, "Passed OCR pre-check! Sending to Gemini AI...")
                
                // 4. AI Timeout + Retry: กำหนดเวลาและพยายามซ้ำ
                val result = processWithAiWithRetry(bitmap)
                if (result.isSuccess) {
                    Log.d(TAG, "Successfully processed transaction from AI")
                    // Save Transaction is handled inside ProcessReceiptImageUseCase already!
                } else {
                    Log.e(TAG, "AI processing failed", result.exceptionOrNull())
                }
                
                receiptDao.insert(ProcessedReceiptEntity(image.uri, image.dateAdded, image.size))
            }
        }.awaitAll()
        Log.d(TAG, "Scan completed")
    }

    private fun queryMediaStore(limit: Int, sinceTimestamp: Long): List<ImageMetadata> {
        val images = mutableListOf<ImageMetadata>()
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE
        )

        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        
        // Filter by dateAdded (convert ms to seconds for MediaStore)
        val selection = "${MediaStore.Images.Media.DATE_ADDED} > ?"
        val selectionArgs = arrayOf((sinceTimestamp / 1000).toString())
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val uri = Uri.withAppendedPath(queryUri, id.toString()).toString()
                    val name = cursor.getString(nameCol) ?: ""
                    val path = cursor.getString(pathCol) ?: ""
                    val dateAdded = cursor.getLong(dateCol)
                    val size = cursor.getLong(sizeCol)
                    
                    // Filter size: 50KB to 5MB
                    if (size in 50_000..5_000_000) {
                        images.add(ImageMetadata(uri, name, path, dateAdded, size))
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore query failed", e)
        }
        
        return images
    }

    private fun calculateScore(image: ImageMetadata): Int {
        var score = 0
        val path = image.relativePath.lowercase()
        val name = image.name.lowercase()
        
        if (path.contains("scb") || path.contains("kbank") || path.contains("krungthai") || path.contains("bbl") || path.contains("kma") || path.contains("bangkokbank")) score += 3
        else if (path.contains("screenshots")) score += 1
        
        if (name.contains("slip") || name.contains("receipt") || name.contains("transfer")) score += 2
        
        // DATE_ADDED Fix: MediaStore ส่งค่าเป็นวินาที (seconds) ต้องคูณ 1000 เพื่อเป็น Milliseconds
        val dateAddedMs = image.dateAdded * 1000L
        val timeDiff = System.currentTimeMillis() - dateAddedMs
        if (timeDiff < 5 * 60 * 1000) score += 1 // ถ่ายภายใน 5 นาที
        
        return score
    }

    private fun loadOptimizedBitmap(uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    var scale = 1
                    var w = info.size.width
                    var h = info.size.height
                    while (w / 2 >= maxWidth && h / 2 >= maxHeight) {
                        w /= 2
                        h /= 2
                        scale *= 2
                    }
                    decoder.setTargetSampleSize(scale)
                }.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                // Downsample logic for old devices could be added here
            }
        } catch (e: Exception) {
            null
        }
    }

    // OCR Cancellable
    private suspend fun performOcrPreCheck(bitmap: Bitmap): Boolean = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        val task = textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (cont.isActive) {
                    val text = visionText.text.lowercase()
                    val hasKeywords = text.contains("โอนเงิน") || text.contains("จำนวนเงิน") || 
                                     text.contains("บาท") || text.contains("amount") ||
                                     text.contains("สำเร็จ") || text.contains("successful")
                    cont.resume(hasKeywords, null)
                }
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(false, null)
            }
            
        cont.invokeOnCancellation {
            // Task cancellation is handled automatically by letting it finish without resuming
        }
    }

    // Timeout + Exponential Backoff Retry
    private suspend fun processWithAiWithRetry(bitmap: Bitmap): Result<Transaction> {
        var currentDelay = 1000L
        val maxRetries = 3
        
        repeat(maxRetries) { attempt ->
            try {
                return withTimeout(15_000L) { // Timeout 15 วินาที
                    geminiUseCase(bitmap)
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Gemini AI Timeout. Attempt ${attempt + 1}")
                if (attempt == maxRetries - 1) return Result.failure(e)
                delay(currentDelay)
                currentDelay *= 2
            } catch (e: Exception) {
                Log.w(TAG, "Gemini AI Error. Attempt ${attempt + 1}", e)
                if (attempt == maxRetries - 1) return Result.failure(e)
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return Result.failure(Exception("Unknown Error"))
    }
}
