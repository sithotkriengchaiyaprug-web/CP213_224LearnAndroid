package com.example.zerotouchbudget.domain.usecase

import com.example.zerotouchbudget.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class CategorySummary(
    val category: String,
    val totalAmount: Double,
    val percentage: Double
)

class GetCategorySummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(startOfDay: Long, endOfDay: Long): Flow<List<CategorySummary>> {
        return transactionRepository.getTodayTransactions(startOfDay, endOfDay).map { transactions ->
            val totalSpent = transactions.sumOf { it.amount }
            if (totalSpent == 0.0) return@map emptyList<CategorySummary>()

            transactions.groupBy { it.category }
                .map { (category, list) ->
                    val amount = list.sumOf { it.amount }
                    CategorySummary(
                        category = category,
                        totalAmount = amount,
                        percentage = (amount / totalSpent) * 100
                    )
                }
                .sortedByDescending { it.totalAmount }
        }
    }
}
