package com.example.zerotouchbudget.domain.repository

import com.example.zerotouchbudget.domain.model.AutoScanSettings
import kotlinx.coroutines.flow.Flow

interface AutoScanSettingsRepository {
    val settings: Flow<AutoScanSettings>

    suspend fun getCurrentSettings(): AutoScanSettings

    suspend fun saveSettings(settings: AutoScanSettings)

    suspend fun updateLastScannedAt(timestampMillis: Long)
}

