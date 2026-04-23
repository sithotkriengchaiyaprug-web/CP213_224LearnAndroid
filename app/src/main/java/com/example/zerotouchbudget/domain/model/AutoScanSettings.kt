package com.example.zerotouchbudget.domain.model

data class AutoScanSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Long = 15L,
    val startAtMillis: Long? = null,
    val source: AutoScanSource = AutoScanSource.SCREENSHOTS,
    val customFolderUri: String? = null,
    val lastScannedAtMillis: Long = 0L
)

