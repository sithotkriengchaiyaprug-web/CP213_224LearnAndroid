package com.example.zerotouchbudget.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zerotouchbudget.domain.model.Transaction
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import com.example.zerotouchbudget.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class DailyTransactionGroup(
    val date: LocalDate,
    val dateString: String,
    val totalAmount: Double,
    val transactions: List<Transaction>
)

data class MonthlyUiState(
    val isLoading: Boolean = true,
    val thisMonthSpending: Double = 0.0,
    val dailyAvg: Double = 0.0,
    val dailyGroups: List<DailyTransactionGroup> = emptyList(),
    val isEmpty: Boolean = false
)

@HiltViewModel
class MonthlyAnalysisViewModel @Inject constructor(
    repository: TransactionRepository
) : ViewModel() {

    private val currentMonthBounds = DateUtils.getCurrentMonthBounds()

    val uiState: StateFlow<MonthlyUiState> = repository
        .getTransactionsBetween(currentMonthBounds.first, currentMonthBounds.second)
        .distinctUntilChanged()
        .map { transactions ->
            if (transactions.isEmpty()) {
                return@map MonthlyUiState(isLoading = false, isEmpty = true)
            }

            val totalSpending = transactions.sumOf { it.amount }

            // Group by LocalDate strictly using Asia/Bangkok Timezone
            val grouped = transactions.groupBy {
                Instant.ofEpochMilli(it.timestamp)
                    .atZone(DateUtils.bangkokZone)
                    .toLocalDate()
            }.toSortedMap(compareByDescending { it })

            val dailyGroups = grouped.map { (date, txs) ->
                DailyTransactionGroup(
                    date = date,
                    dateString = DateUtils.formatToDayString(date.atStartOfDay(DateUtils.bangkokZone).toInstant().toEpochMilli()),
                    totalAmount = txs.sumOf { it.amount },
                    transactions = txs.sortedByDescending { it.timestamp }
                )
            }

            val daysWithTransactions = dailyGroups.size
            val avg = if (daysWithTransactions > 0) totalSpending / daysWithTransactions else 0.0

            MonthlyUiState(
                isLoading = false,
                thisMonthSpending = totalSpending,
                dailyAvg = avg,
                dailyGroups = dailyGroups,
                isEmpty = false
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MonthlyUiState(isLoading = true)
        )
}
