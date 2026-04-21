package com.example.zerotouchbudget.domain.usecase

import com.example.zerotouchbudget.domain.model.Transaction
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import com.example.zerotouchbudget.domain.util.DateUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTodayTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(dateString: String): Flow<List<Transaction>> {
        val (start, end) = DateUtils.getDayBounds(dateString)
        return repository.getTodayTransactions(start, end)
    }
}