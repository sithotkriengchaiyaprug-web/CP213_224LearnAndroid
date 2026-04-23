package com.example.zerotouchbudget.domain.model

data class ProcessedReceiptImage(
    val imageUri: String,
    val displayName: String,
    val relativePath: String,
    val folderName: String,
    val processedAtMillis: Long,
    val ocrPassed: Boolean,
    val aiProcessed: Boolean
)

