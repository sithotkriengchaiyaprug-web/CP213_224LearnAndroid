package com.example.zerotouchbudget

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.zerotouchbudget.data.service.scheduleMidnightReset
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import com.example.zerotouchbudget.data.service.scheduleImageScanWorker

@HiltAndroidApp
class BudgetApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleMidnightReset(this)
        scheduleImageScanWorker(this)
    }
}