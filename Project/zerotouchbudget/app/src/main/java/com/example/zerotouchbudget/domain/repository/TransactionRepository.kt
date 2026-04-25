package com.example.zerotouchbudget.domain.repository

import com.example.zerotouchbudget.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTodayTransactions(startOfDay: Long, endOfDay: Long): Flow<List<Transaction>>
    // Generic date-range query used by summary and widget paths.
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun getTotalSpentForDate(startOfDay: Long, endOfDay: Long): Double
    suspend fun deleteTransaction(transaction: Transaction)
}
