package com.example.zerotouchbudget.presentation.home

import com.example.zerotouchbudget.domain.model.Transaction

data class HomeUiState(
    val remainingBudget: Double = 0.0,
    val dailyBudgetLimit: Double = 100.0,
    val totalSpentToday: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val editingTransaction: Transaction? = null
) {
    val spentPercentage: Float
        get() = if (dailyBudgetLimit > 0) {
            (totalSpentToday / dailyBudgetLimit).toFloat().coerceIn(0f, 1f)
        } else 0f

    val isOverBudget: Boolean
        get() = remainingBudget < 0
}