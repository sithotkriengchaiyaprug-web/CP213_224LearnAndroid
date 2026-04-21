package com.example.zerotouchbudget.domain.usecase

import com.example.zerotouchbudget.domain.repository.DailySummaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class MonthlySummary(
    val totalSpent: Double,
    val totalBudget: Double,
    val daysOverBudget: Int,
    val averageDailySpending: Double
)

class GetMonthlySummaryUseCase @Inject constructor(
    private val dailySummaryRepository: DailySummaryRepository
) {
    operator fun invoke(startDate: String, endDate: String): Flow<MonthlySummary> {
        return dailySummaryRepository.getSummariesInRange(startDate, endDate).map { summaries ->
            val totalSpent = summaries.sumOf { it.totalSpent }
            val totalBudget = summaries.sumOf { it.budgetLimit }
            val daysOverBudget = summaries.count { it.totalSpent > it.budgetLimit }
            val avg = if (summaries.isNotEmpty()) totalSpent / summaries.size else 0.0

            MonthlySummary(
                totalSpent = totalSpent,
                totalBudget = totalBudget,
                daysOverBudget = daysOverBudget,
                averageDailySpending = avg
            )
        }
    }
}
