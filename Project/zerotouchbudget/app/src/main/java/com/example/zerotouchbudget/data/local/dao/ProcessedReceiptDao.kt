package com.example.zerotouchbudget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zerotouchbudget.data.local.entity.ProcessedReceiptEntity

@Dao
interface ProcessedReceiptDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(receipt: ProcessedReceiptEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM processed_receipts WHERE imageUri = :uri)")
    suspend fun isAlreadyProcessed(uri: String): Boolean
}
