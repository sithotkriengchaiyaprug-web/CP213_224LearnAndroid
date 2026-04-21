package com.example.zerotouchbudget.domain.usecase

import com.example.zerotouchbudget.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * คำนวณงบประมาณที่เหลืออยู่
 * Logic: Remaining = BudgetLimit - TotalSpent
 */
class CalculateRemainingBudgetUseCase @Inject constructor(
    private val getBudgetForDateUseCase: GetBudgetForDateUseCase,
    private val getTodayTransactionsUseCase: GetTodayTransactionsUseCase
) {
    operator fun invoke(dateString: String): Flow<Double> {
        return combine(
            getBudgetForDateUseCase(dateString),
            getTodayTransactionsUseCase(dateString)
        ) { budgetLimit, transactions ->
            val totalSpent = transactions.sumOf { it.amount }
            budgetLimit - totalSpent
        }
    }
}