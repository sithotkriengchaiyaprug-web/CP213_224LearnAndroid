package com.example.zerotouchbudget.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zerotouchbudget.domain.usecase.GetDailySummaryUseCase
import com.example.zerotouchbudget.domain.usecase.GetTodayTransactionsUseCase
import com.example.zerotouchbudget.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodayTransactionsUseCase: GetTodayTransactionsUseCase,
    private val getDailySummaryUseCase: GetDailySummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadTodayData()
    }

    private fun loadTodayData() {
        viewModelScope.launch {
            val today = DateUtils.getCurrentDateString()

            combine(
                getDailySummaryUseCase(today),
                getTodayTransactionsUseCase(today)
            ) { summary, transactions ->
                val totalSpent = transactions.sumOf { it.amount }
                val budgetLimit = summary?.budgetLimit ?: 100.0
                val remaining = budgetLimit - totalSpent

                HomeUiState(
                    remainingBudget = remaining,
                    dailyBudgetLimit = budgetLimit,
                    totalSpentToday = totalSpent,
                    recentTransactions = transactions,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}