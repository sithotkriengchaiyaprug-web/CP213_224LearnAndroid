package com.example.zerotouchbudget.domain.usecase

import com.example.zerotouchbudget.domain.model.DailySummary
import com.example.zerotouchbudget.domain.repository.DailySummaryRepository
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import com.example.zerotouchbudget.domain.util.DateUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CalculateDailySurplusUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val summaryRepository: DailySummaryRepository
) {
    suspend operator fun invoke() {
        val yesterday = DateUtils.getYesterdayDateString()
        val (start, end) = DateUtils.getDayBounds(yesterday)

        val totalSpent = transactionRepository.getTotalSpentForDate(start, end)
<<<<<<< ours
        val budgetLimit = summaryRepository.getSummaryForDate(yesterday).first()?.budgetLimit ?: 100.0
        val transactionCount = transactionRepository.getTodayTransactions(start, end).first().size
=======
        val budgetLimit = summaryRepository
            .getSummaryForDate(yesterday)
            .first()
            ?.budgetLimit
            ?: 100.0
>>>>>>> theirs

        val summary = DailySummary(
            date = yesterday,
            budgetLimit = budgetLimit,
            totalSpent = totalSpent,
            surplus = budgetLimit - totalSpent,
            transactionCount = transactionCount
        )

        summaryRepository.saveSummary(summary)
    }
}
