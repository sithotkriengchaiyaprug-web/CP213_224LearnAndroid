package com.example.zerotouchbudget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zerotouchbudget.data.local.entity.ProcessedReceiptImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessedReceiptImageDao {

    @Query("SELECT COUNT(*) FROM processed_receipt_images WHERE imageUri = :imageUri")
    suspend fun countByImageUri(imageUri: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProcessedReceiptImageEntity)

    @Query(
        """
        SELECT folderName, COUNT(*) AS itemCount
        FROM processed_receipt_images
        GROUP BY folderName
        ORDER BY itemCount DESC
        """
    )
    suspend fun getFolderCounts(): List<FolderCountRow>

    data class FolderCountRow(
        val folderName: String,
        val itemCount: Int
    )
}

