package com.example.zerotouchbudget.presentation.home

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zerotouchbudget.domain.model.Transaction
import com.example.zerotouchbudget.domain.model.TransactionSource
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import com.example.zerotouchbudget.domain.usecase.GetDailySummaryUseCase
import com.example.zerotouchbudget.domain.usecase.GetTodayTransactionsUseCase
import com.example.zerotouchbudget.domain.usecase.ProcessReceiptImageUseCase
import com.example.zerotouchbudget.domain.util.DateUtils
import com.example.zerotouchbudget.presentation.widget.BudgetWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodayTransactionsUseCase: GetTodayTransactionsUseCase,
    private val getDailySummaryUseCase: GetDailySummaryUseCase,
    private val processReceiptImageUseCase: ProcessReceiptImageUseCase,
    private val repository: TransactionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _isAutoScanEnabled = MutableStateFlow(true)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        val today = DateUtils.getCurrentDateString()
        
        combine(
            getDailySummaryUseCase(today),
            getTodayTransactionsUseCase(today),
            _isLoading,
            _isAutoScanEnabled
        ) { summary, transactions, loading, autoScan ->
            val totalSpent = transactions.sumOf { it.amount }
            val budgetLimit = summary?.budgetLimit ?: 100.0
            val remaining = budgetLimit - totalSpent

            HomeUiState(
                remainingBudget = remaining,
                dailyBudgetLimit = budgetLimit,
                totalSpentToday = totalSpent,
                recentTransactions = transactions,
                isLoading = loading,
                isAutoScanEnabled = autoScan
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun toggleAutoScan() {
        _isAutoScanEnabled.value = !_isAutoScanEnabled.value
    }

    fun processReceipt(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = processReceiptImageUseCase(bitmap)
                if (result.isSuccess) {
                    updateWidget()
                }
            } catch (e: Exception) {
                // Error handling can be added to UI state
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addManualTransaction(amount: Double, brand: String, category: String) {
        viewModelScope.launch {
            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                amount = amount,
                brand = brand,
                category = category,
                timestamp = System.currentTimeMillis(),
                source = TransactionSource.MANUAL,
                note = "Added manually"
            )
            repository.insertTransaction(transaction)
            updateWidget()
        }
    }

    fun editTransaction(transaction: Transaction, newAmount: Double, newBrand: String, newCategory: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            val updated = Transaction(
                id = transaction.id,
                amount = newAmount,
                brand = transaction.brand, // Keep original brand if needed
                category = newCategory,
                timestamp = transaction.timestamp,
                source = transaction.source,
                note = transaction.note
            )
            repository.insertTransaction(updated)
            updateWidget()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            updateWidget()
        }
    }

    private suspend fun updateWidget() {
        BudgetWidget().updateAll(context)
    }
}
