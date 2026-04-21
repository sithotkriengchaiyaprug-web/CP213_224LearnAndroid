package com.example.zerotouchbudget.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zerotouchbudget.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query(
        """
        SELECT * FROM transactions 
        WHERE timestamp >= :startOfDay AND timestamp < :endOfDay 
        ORDER BY timestamp DESC
        """
    )
    fun getTransactionsForDate(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0) 
        FROM transactions 
        WHERE timestamp >= :startOfDay AND timestamp < :endOfDay
        """
    )
    suspend fun getTotalSpentForDate(startOfDay: Long, endOfDay: Long): Double

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Delete
    suspend fun delete(transaction: TransactionEntity)
}