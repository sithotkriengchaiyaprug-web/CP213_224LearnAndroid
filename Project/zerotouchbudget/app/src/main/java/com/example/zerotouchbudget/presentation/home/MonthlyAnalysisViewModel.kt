package com.example.zerotouchbudget.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zerotouchbudget.domain.model.Transaction
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import com.example.zerotouchbudget.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DailyTransactionGroup(
    val dateString: String,
    val timestamp: Long,
    val totalAmount: Double,
    val transactions: List<Transaction>
)

data class MonthlyUiState(
    val isLoading: Boolean = true,
    val thisMonthSpending: Double = 0.0,
    val dailyAvg: Double = 0.0,
    val dailyGroups: List<DailyTransactionGroup> = emptyList()
)

@HiltViewModel
class MonthlyAnalysisViewModel @Inject constructor(
    repository: TransactionRepository
) : ViewModel() {

    private val currentMonthBounds = DateUtils.getCurrentMonthBounds()

    val uiState: StateFlow<MonthlyUiState> = repository
        .getTransactionsBetween(currentMonthBounds.first, currentMonthBounds.second)
        .map { transactions ->
            val totalSpending = transactions.sumOf { it.amount }

            // Group by date string (e.g., "April 25, 2024")
            val grouped = transactions.groupBy { DateUtils.formatToDayString(it.timestamp) }
            
            val dailyGroups = grouped.map { (dateStr, txs) ->
                DailyTransactionGroup(
                    dateString = dateStr,
                    timestamp = txs.first().timestamp, // Used for sorting
                    totalAmount = txs.sumOf { it.amount },
                    transactions = txs.sortedByDescending { it.timestamp }
                )
            }.sortedByDescending { it.timestamp }

            val daysWithTransactions = dailyGroups.size
            val avg = if (daysWithTransactions > 0) totalSpending / daysWithTransactions else 0.0

            MonthlyUiState(
                isLoading = false,
                thisMonthSpending = totalSpending,
                dailyAvg = avg,
                dailyGroups = dailyGroups
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MonthlyUiState(isLoading = true)
        )
}
