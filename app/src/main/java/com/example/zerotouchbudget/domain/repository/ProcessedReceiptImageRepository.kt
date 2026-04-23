package com.example.zerotouchbudget.domain.repository

import com.example.zerotouchbudget.domain.model.ProcessedReceiptImage

interface ProcessedReceiptImageRepository {
    suspend fun isProcessed(imageUri: String): Boolean
    suspend fun markProcessed(image: ProcessedReceiptImage)
    suspend fun getFolderCounts(): Map<String, Int>
}

