package com.example.zerotouchbudget.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_receipts")
data class ProcessedReceiptEntity(
    @PrimaryKey
    val imageUri: String,
    val dateAdded: Long,
    val fileSize: Long,
    val processedAt: Long = System.currentTimeMillis()
)
