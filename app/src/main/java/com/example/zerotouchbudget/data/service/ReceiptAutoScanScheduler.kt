package com.example.zerotouchbudget.data.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.zerotouchbudget.domain.model.AutoScanSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptAutoScanScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun schedule(settings: AutoScanSettings) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(BOOTSTRAP_WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)

        if (!settings.enabled) return

        val delayMillis = (settings.startAtMillis ?: System.currentTimeMillis())
            .minus(System.currentTimeMillis())
            .coerceAtLeast(0L)

        val bootstrapWork = OneTimeWorkRequestBuilder<ReceiptAutoScanBootstrapWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            BOOTSTRAP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            bootstrapWork
        )
    }

    fun schedulePeriodic(settings: AutoScanSettings) {
        val periodicWork = PeriodicWorkRequestBuilder<ReceiptAutoScanWorker>(
            settings.intervalMinutes,
            TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWork
        )
    }

    fun cancel() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(BOOTSTRAP_WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    private companion object {
        const val BOOTSTRAP_WORK_NAME = "receipt_auto_scan_bootstrap"
        const val PERIODIC_WORK_NAME = "receipt_auto_scan_periodic"
    }
}
