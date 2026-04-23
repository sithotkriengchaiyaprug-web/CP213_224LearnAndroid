package com.example.zerotouchbudget.data.repository

import com.example.zerotouchbudget.data.local.dao.ProcessedReceiptImageDao
import com.example.zerotouchbudget.data.local.entity.ProcessedReceiptImageEntity
import com.example.zerotouchbudget.domain.model.ProcessedReceiptImage
import com.example.zerotouchbudget.domain.repository.ProcessedReceiptImageRepository
import javax.inject.Inject

class ProcessedReceiptImageRepositoryImpl @Inject constructor(
    private val processedReceiptImageDao: ProcessedReceiptImageDao
) : ProcessedReceiptImageRepository {

    override suspend fun isProcessed(imageUri: String): Boolean {
        return processedReceiptImageDao.countByImageUri(imageUri) > 0
    }

    override suspend fun markProcessed(image: ProcessedReceiptImage) {
        processedReceiptImageDao.insert(
            ProcessedReceiptImageEntity(
                imageUri = image.imageUri,
                displayName = image.displayName,
                relativePath = image.relativePath,
                folderName = image.folderName,
                processedAtMillis = image.processedAtMillis,
                ocrPassed = image.ocrPassed,
                aiProcessed = image.aiProcessed
            )
        )
    }

    override suspend fun getFolderCounts(): Map<String, Int> {
        return processedReceiptImageDao.getFolderCounts()
            .associate { row -> row.folderName to row.itemCount }
    }
}

