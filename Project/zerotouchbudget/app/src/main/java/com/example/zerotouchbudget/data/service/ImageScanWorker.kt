package com.example.zerotouchbudget.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.zerotouchbudget.data.local.AppPreferences
import com.example.zerotouchbudget.domain.usecase.ProcessReceiptImageUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class ImageScanWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val processReceiptImageUseCase: ProcessReceiptImageUseCase,
    private val appPreferences: AppPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!appPreferences.isAutoScanEnabled) {
            Log.d(TAG, "Auto scan is disabled. Skipping work.")
            return@withContext Result.success()
        }

        try {
            Log.d(TAG, "ImageScanWorker started. Checking for new media...")
            val latestImage = getLatestImageFromGallery()
            
            if (latestImage == null) {
                Log.d(TAG, "No images found in gallery.")
                return@withContext Result.success()
            }

            val (id, uri) = latestImage
            val lastProcessedId = appPreferences.lastScannedImageId

            Log.d(TAG, "Latest Image ID: ${id}, Last Processed ID: ${lastProcessedId}")

            if (id <= lastProcessedId) {
                Log.d(TAG, "Image $id already processed or older. Skipping.")
                return@withContext Result.success()
            }

            // This is a new image, process it
            Log.d(TAG, "Found new image $id. Processing...")
            
            val bitmap = loadBitmapFromUri(uri)
            if (bitmap != null) {
                val result = processReceiptImageUseCase(bitmap)
                result.onSuccess {
                    Log.d(TAG, "Successfully auto-scanned receipt: ${it.brand} - ${it.amount}")
                }.onFailure {
                    Log.e(TAG, "Failed to auto-scan receipt: ${it.message}")
                }
            }
            
            // Mark as processed regardless of success/failure so we don't get stuck on a bad image
            appPreferences.lastScannedImageId = id
            
            // Re-schedule to observe the next change
            scheduleImageScanWorker(context)
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in ImageScanWorker", e)
            Result.retry()
        }
    }

    private fun getLatestImageFromGallery(): Pair<Long, Uri>? {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )

        // Query external content
        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Images.Media._ID} DESC"

        context.contentResolver.query(
            queryUri,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(queryUri, id.toString())
                return Pair(id, contentUri)
            }
        }
        return null
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from URI", e)
            null
        }
    }

    companion object {
        private const val TAG = "ImageScanWorker"
    }
}

fun scheduleImageScanWorker(context: Context) {
    val constraints = androidx.work.Constraints.Builder()
        .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
        .build()

    val request = androidx.work.OneTimeWorkRequestBuilder<ImageScanWorker>()
        .setConstraints(constraints)
        .build()

    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
        "ImageScanWorker",
        androidx.work.ExistingWorkPolicy.REPLACE,
        request
    )
}
