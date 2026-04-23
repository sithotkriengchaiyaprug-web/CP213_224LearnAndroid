package com.example.zerotouchbudget.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.zerotouchbudget.domain.model.AutoScanSettings
import com.example.zerotouchbudget.domain.model.AutoScanSource
import com.example.zerotouchbudget.domain.repository.AutoScanSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.autoScanDataStore by preferencesDataStore(name = "auto_scan_settings")

class AutoScanSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AutoScanSettingsRepository {

    override val settings: Flow<AutoScanSettings> = context.autoScanDataStore.data.map { prefs ->
        AutoScanSettings(
            enabled = prefs[ENABLED_KEY] ?: false,
            intervalMinutes = prefs[INTERVAL_MINUTES_KEY] ?: 15L,
            startAtMillis = prefs[START_AT_MILLIS_KEY]?.takeIf { it > 0L },
            source = AutoScanSource.fromStorage(prefs[SOURCE_KEY]),
            customFolderUri = prefs[CUSTOM_FOLDER_URI_KEY],
            lastScannedAtMillis = prefs[LAST_SCANNED_AT_KEY] ?: 0L
        )
    }

    override suspend fun getCurrentSettings(): AutoScanSettings {
        return settings.first()
    }

    override suspend fun saveSettings(settings: AutoScanSettings) {
        context.autoScanDataStore.edit { prefs ->
            prefs[ENABLED_KEY] = settings.enabled
            prefs[INTERVAL_MINUTES_KEY] = settings.intervalMinutes
            prefs[START_AT_MILLIS_KEY] = settings.startAtMillis ?: 0L
            prefs[SOURCE_KEY] = settings.source.name
            if (settings.customFolderUri.isNullOrBlank()) {
                prefs.remove(CUSTOM_FOLDER_URI_KEY)
            } else {
                prefs[CUSTOM_FOLDER_URI_KEY] = settings.customFolderUri
            }
            prefs[LAST_SCANNED_AT_KEY] = settings.lastScannedAtMillis
        }
    }

    override suspend fun updateLastScannedAt(timestampMillis: Long) {
        context.autoScanDataStore.edit { prefs ->
            prefs[LAST_SCANNED_AT_KEY] = timestampMillis
        }
    }

    private companion object {
        val ENABLED_KEY = booleanPreferencesKey("enabled")
        val INTERVAL_MINUTES_KEY = longPreferencesKey("interval_minutes")
        val START_AT_MILLIS_KEY = longPreferencesKey("start_at_millis")
        val SOURCE_KEY = stringPreferencesKey("source")
        val CUSTOM_FOLDER_URI_KEY = stringPreferencesKey("custom_folder_uri")
        val LAST_SCANNED_AT_KEY = longPreferencesKey("last_scanned_at_millis")
    }
}

