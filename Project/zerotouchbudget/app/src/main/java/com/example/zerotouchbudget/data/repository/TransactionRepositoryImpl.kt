package com.example.zerotouchbudget.data.repository

import com.example.zerotouchbudget.data.local.dao.TransactionDao
import com.example.zerotouchbudget.data.local.mapper.toDomainModel
import com.example.zerotouchbudget.data.local.mapper.toEntity
import com.example.zerotouchbudget.domain.model.Transaction
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getTodayTransactions(
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<Transaction>> {
        return getTransactionsBetween(startOfDay, endOfDay)
    }

    override fun getTransactionsBetween(start: Long, end: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBetween(start, end)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
    }

    override suspend fun getTotalSpentForDate(startOfDay: Long, endOfDay: Long): Double {
        return transactionDao.getTotalSpentForDate(startOfDay, endOfDay)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
    }
}
