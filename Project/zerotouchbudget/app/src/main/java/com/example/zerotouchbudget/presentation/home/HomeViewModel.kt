package com.example.zerotouchbudget.presentation.home

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zerotouchbudget.data.local.AppPreferences
import com.example.zerotouchbudget.data.service.scanner.SmartReceiptScanner
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodayTransactionsUseCase: GetTodayTransactionsUseCase,
    private val getDailySummaryUseCase: GetDailySummaryUseCase,
    private val processReceiptImageUseCase: ProcessReceiptImageUseCase,
    private val smartScanner: SmartReceiptScanner,
    private val repository: TransactionRepository,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val todayDate = DateUtils.getCurrentDateString()
    private val _isLoading = MutableStateFlow(false)
    private val _isAutoScanEnabled = MutableStateFlow(appPreferences.isAutoScanEnabled)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        getDailySummaryUseCase(todayDate)
            .catch { throwable ->
                Log.e(TAG, "Daily summary flow failed", throwable)
                emit(null)
            },
        getTodayTransactionsUseCase(todayDate)
            .catch { throwable ->
                Log.e(TAG, "Transaction flow failed", throwable)
                emit(emptyList())
            },
        _isLoading,
        _isAutoScanEnabled,
        _errorMessage
    ) { summary, transactions, loading, autoScan, errorMessage ->
        val totalSpent = transactions.sumOf { it.amount }
        val budgetLimit = summary?.budgetLimit ?: 100.0
        val remaining = budgetLimit - totalSpent

        Log.d(
            TAG,
            "UI state recalculated: count=${transactions.size}, totalSpent=$totalSpent, " +
                "remaining=$remaining, loading=$loading"
        )

        HomeUiState(
            remainingBudget = remaining,
            dailyBudgetLimit = budgetLimit,
            totalSpentToday = totalSpent,
            recentTransactions = transactions,
            isLoading = loading,
            errorMessage = errorMessage,
            isAutoScanEnabled = autoScan
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            isLoading = false,
            isAutoScanEnabled = appPreferences.isAutoScanEnabled
        )
    )

    fun toggleAutoScan() {
        val newState = !_isAutoScanEnabled.value
        _isAutoScanEnabled.value = newState
        appPreferences.isAutoScanEnabled = newState
        Log.d(TAG, "Auto-scan toggled: $newState")
    }

    fun scanExistingImages() {
        viewModelScope.launch {
            Log.d(TAG, "scanExistingImages() started")
            _errorMessage.value = null
            _isLoading.value = true
            try {
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                withContext(Dispatchers.IO) {
                    smartScanner.scan(limit = 200, sinceTimestamp = sevenDaysAgo, forceRescan = true)
                }
                updateWidget()
                Log.d(TAG, "scanExistingImages() completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "scanExistingImages() failed", e)
                _errorMessage.value = e.message ?: "Failed to scan existing images"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "scanExistingImages() finished; loading=false")
            }
        }
    }

    fun addManualTransaction(amount: Double, brand: String, category: String) {
        viewModelScope.launch {
            Log.d(TAG, "addManualTransaction() amount=$amount brand=$brand category=$category")
            _errorMessage.value = null
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
            Log.d(TAG, "editTransaction() id=${transaction.id} newAmount=$newAmount newBrand=$newBrand newCategory=$newCategory")
            _errorMessage.value = null
            repository.deleteTransaction(transaction)
            val updated = Transaction(
                id = transaction.id,
                amount = newAmount,
                brand = newBrand.ifBlank { transaction.brand },
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
            Log.d(TAG, "deleteTransaction() id=${transaction.id}")
            _errorMessage.value = null
            repository.deleteTransaction(transaction)
            updateWidget()
        }
    }

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            Log.d(TAG, "processImage() started. bitmap=${bitmap.width}x${bitmap.height}, bytes=${bitmap.byteCount}")
            _errorMessage.value = null
            _isLoading.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    processReceiptImageUseCase(bitmap)
                }

                result
                    .onSuccess { transaction ->
                        Log.d(
                            TAG,
                            "processImage() success. transactionId=${transaction.id}, amount=${transaction.amount}, brand=${transaction.brand}"
                        )
                        updateWidget()
                        Log.d(TAG, "Widget updated after receipt insert")
                    }
                    .onFailure { throwable ->
                        Log.e(TAG, "processImage() failed", throwable)
                        _errorMessage.value = throwable.message ?: "Failed to process receipt image"
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in processImage()", e)
                _errorMessage.value = e.message ?: "Unexpected error while processing receipt"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "processImage() finished; loading=false")
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private suspend fun updateWidget() {
        BudgetWidget().updateAll(context)
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
