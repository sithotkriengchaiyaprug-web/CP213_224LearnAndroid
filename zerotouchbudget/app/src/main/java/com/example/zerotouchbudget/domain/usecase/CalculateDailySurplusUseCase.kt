package com.example.zerotouchbudget.domain.usecase

import com.example.zerotouchbudget.domain.model.DailySummary
import com.example.zerotouchbudget.domain.repository.DailySummaryRepository
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import com.example.zerotouchbudget.domain.util.DateUtils
import javax.inject.Inject

class CalculateDailySurplusUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val summaryRepository: DailySummaryRepository
) {
    suspend operator fun invoke() {
        val yesterday = DateUtils.getYesterdayDateString()
        val (start, end) = DateUtils.getDayBounds(yesterday)

        val totalSpent = transactionRepository.getTotalSpentForDate(start, end)
        val budgetLimit = 100.0

        val summary = DailySummary(
            date = yesterday,
            budgetLimit = budgetLimit,
            totalSpent = totalSpent,
            surplus = budgetLimit - totalSpent,
            transactionCount = 0
        )

        summaryRepository.saveSummary(summary)
    }
}