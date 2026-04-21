package com.example.zerotouchbudget.data.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.zerotouchbudget.domain.usecase.CalculateDailySurplusUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val calculateDailySurplusUseCase: CalculateDailySurplusUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            calculateDailySurplusUseCase()
            scheduleMidnightReset(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

fun scheduleMidnightReset(context: Context) {
    val currentDate = Calendar.getInstance()
    val midnight = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_MONTH, 1)
    }

    val initialDelay = midnight.timeInMillis - currentDate.timeInMillis

    val dailyWorkRequest = OneTimeWorkRequestBuilder<DailyResetWorker>()
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "DailyResetWork",
        ExistingWorkPolicy.REPLACE,
        dailyWorkRequest
    )
}