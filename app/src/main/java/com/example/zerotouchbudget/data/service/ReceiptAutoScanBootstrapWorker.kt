package com.example.zerotouchbudget.data.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.zerotouchbudget.domain.repository.AutoScanSettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReceiptAutoScanBootstrapWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val autoScanSettingsRepository: AutoScanSettingsRepository,
    private val autoScanScheduler: ReceiptAutoScanScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val settings = autoScanSettingsRepository.getCurrentSettings()
            if (!settings.enabled) {
                autoScanScheduler.cancel()
                return Result.success()
            }

            autoScanScheduler.schedulePeriodic(settings)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

