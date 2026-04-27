package com.example.zerotouchbudget.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var lastScannedImageId: Long
        get() = prefs.getLong(KEY_LAST_SCANNED_IMAGE_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_SCANNED_IMAGE_ID, value).apply()

    var isAutoScanEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SCAN_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SCAN_ENABLED, value).apply()

    var dailyBudget: Double
        get() = prefs.getFloat(KEY_DAILY_BUDGET, 500f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_DAILY_BUDGET, value.toFloat()).apply()

    companion object {
        private const val PREF_NAME = "zero_touch_budget_prefs"
        private const val KEY_LAST_SCANNED_IMAGE_ID = "last_scanned_image_id"
        private const val KEY_AUTO_SCAN_ENABLED = "auto_scan_enabled"
        private const val KEY_DAILY_BUDGET = "daily_budget"
    }
}
