package com.example.zerotouchbudget.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_receipt_images")
data class ProcessedReceiptImageEntity(
    @PrimaryKey
    val imageUri: String,
    val displayName: String,
    val relativePath: String,
    val folderName: String,
    val processedAtMillis: Long,
    val ocrPassed: Boolean,
    val aiProcessed: Boolean
)

