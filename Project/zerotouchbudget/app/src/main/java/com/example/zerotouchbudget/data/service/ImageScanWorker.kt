package com.example.zerotouchbudget.data.service

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.zerotouchbudget.data.local.AppPreferences
import com.example.zerotouchbudget.data.service.scanner.SmartReceiptScanner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ImageScanWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val smartScanner: SmartReceiptScanner,
    private val appPreferences: AppPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (com.google.firebase.FirebaseApp.getApps(applicationContext).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(applicationContext)
        }

        if (!appPreferences.isAutoScanEnabled) {
            Log.d(TAG, "Auto scan is disabled. Skipping work.")
            return Result.success()
        }

        return try {
            Log.d(TAG, "ImageScanWorker started. Starting smart scan...")
            
            // Limit to 50 for background worker to save battery
            // Use lastScanImageId conceptually as timestamp or just track it inside the scanner
            // Here we just pass 0 to let the scanner check deduplication via Room
            smartScanner.scan(limit = 50, sinceTimestamp = 0)
            
            // Re-schedule
            scheduleImageScanWorker(context)
            
            Result.success()
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Network or IO Error in ImageScanWorker", e)
            if (runAttemptCount > 3) Result.failure() else Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Permanent error in ImageScanWorker", e)
            Result.failure()
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
