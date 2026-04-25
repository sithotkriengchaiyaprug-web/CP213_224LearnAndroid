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

data class ScanSummary(
    val totalFound: Int = 0,
    val skippedDedup: Int = 0,
    val skippedScore: Int = 0,
    val failedOcr: Int = 0,
    val failedAi: Int = 0,
    val success: Int = 0
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

    suspend fun scan(limit: Int = 100, sinceTimestamp: Long = 0, forceRescan: Boolean = false): ScanSummary = withContext(scanDispatcher) {
        Log.d(TAG, "Starting scan limit=$limit since=$sinceTimestamp forceRescan=$forceRescan")
        val images = queryMediaStore(limit, sinceTimestamp)
        
        var skippedDedup = 0
        var skippedScore = 0
        var failedOcr = 0
        var notReceipt = 0
        var failedAi = 0
        var success = 0
        var aiCallCount = 0
        val AI_BATCH_LIMIT = 5 // ไม่เกิน 5 รูปต่อ session เพื่อไม่ let rate limit

        for (image in images) {
            // หยุดส่ง AI เมื่อครบ batch limit
            if (aiCallCount >= AI_BATCH_LIMIT) {
                Log.d(TAG, "AI batch limit reached ($AI_BATCH_LIMIT). Stopping.")
                break
            }

            // Deduplication Check
            if (!forceRescan && receiptDao.isAlreadyProcessed(image.uri)) {
                skippedDedup++
                continue
            }

            // Score Filter — รูปที่ไม่น่าใช่สลิปเลย
            val score = calculateScore(image)
            if (score < 1) {
                Log.d(TAG, "Skipping ${image.name} (score: $score)")
                skippedScore++
                continue
            }

            Log.d(TAG, "Processing ${image.name} (score: $score)")
            
            // Bitmap Optimization
            val bitmap = loadOptimizedBitmap(Uri.parse(image.uri), maxWidth = 1024, maxHeight = 1024)
            if (bitmap == null) {
                Log.e(TAG, "Failed to load bitmap for ${image.uri}")
                failedOcr++
                continue
            }

            // Rate limiting: delay 5s between AI calls
            if (aiCallCount > 0) delay(5_000L)

            // ส่งให้ Gemini AI ตัดสินโดยตรง
            Log.d(TAG, "Sending ${image.name} to Gemini AI (call ${aiCallCount + 1}/$AI_BATCH_LIMIT)...")
            aiCallCount++
            
            // AI Timeout + Retry
            val result = processWithAiWithRetry(bitmap)
            if (result.isSuccess) {
                Log.d(TAG, "Successfully processed transaction from AI")
                success++
            } else {
                val ex = result.exceptionOrNull()
                if (ex is com.example.zerotouchbudget.domain.usecase.NotAReceiptException) {
                    Log.d(TAG, "Not a receipt: ${image.name}")
                    notReceipt++
                } else {
                    Log.e(TAG, "AI processing failed for ${image.name}", ex)
                    failedAi++
                }
            }
            
            receiptDao.insert(ProcessedReceiptEntity(image.uri, image.dateAdded, image.size))
        }
        Log.d(TAG, "Scan completed")
        ScanSummary(images.size, skippedDedup, skippedScore, notReceipt, failedAi, success)
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
        else if (path.contains("screenshots") || path.contains("line") || path.contains("pictures") || path.contains("download")) score += 2
        else score += 1 // ให้คะแนนพื้นฐานไว้ก่อน
        
        if (name.contains("slip") || name.contains("receipt") || name.contains("transfer") || 
            name.contains("statement") || name.contains("kbank") || name.contains("scb") || 
            name.contains("krungthai") || name.contains("ktb") || name.contains("bbl") || 
            name.contains("ttb") || name.contains("kma") || name.contains("gsb")) {
            score += 2
        }
        
        // DATE_ADDED Fix: MediaStore ส่งค่าเป็นวินาที (seconds) ต้องคูณ 1000 เพื่อเป็น Milliseconds
        val dateAddedMs = image.dateAdded * 1000L
        val timeDiff = System.currentTimeMillis() - dateAddedMs
        if (timeDiff < 5 * 60 * 1000) score += 2 // ถ่ายภายใน 5 นาที ให้คะแนนสูงขึ้นเลย
        else if (timeDiff < 24 * 60 * 60 * 1000) score += 1 // ภายใน 1 วัน
        
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
                                     text.contains("สำเร็จ") || text.contains("successful") ||
                                     text.contains("ยอดเงิน") || text.contains("จาก") || 
                                     text.contains("ไปยัง") || text.contains("รายการ") ||
                                     text.contains("thb")
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

    // Timeout + Exponential Backoff Retry with Rate Limit detection
    private suspend fun processWithAiWithRetry(bitmap: Bitmap): Result<Transaction> {
        var currentDelay = 2000L
        val maxRetries = 3
        
        repeat(maxRetries) { attempt ->
            try {
                return withTimeout(20_000L) { // Timeout 20 วินาที
                    geminiUseCase(bitmap)
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Gemini AI Timeout. Attempt ${attempt + 1}")
                if (attempt == maxRetries - 1) return Result.failure(e)
                delay(currentDelay)
                currentDelay *= 2
            } catch (e: Exception) {
                val isRateLimit = e.message?.contains("quota", ignoreCase = true) == true ||
                    e.message?.contains("429") == true ||
                    e.message?.contains("rate", ignoreCase = true) == true
                if (isRateLimit) {
                    Log.w(TAG, "Rate limit hit! Waiting 60s before retry ${attempt + 1}...")
                    delay(60_000L) // รอ 60 วินาที แล้ว retry
                } else {
                    Log.w(TAG, "Gemini AI Error. Attempt ${attempt + 1}", e)
                    if (attempt == maxRetries - 1) return Result.failure(e)
                    delay(currentDelay)
                    currentDelay *= 2
                }
            }
        }
        return Result.failure(Exception("Unknown Error"))
    }
}
