package com.example.zerotouchbudget.presentation.settings

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zerotouchbudget.domain.repository.DailySummaryRepository
import com.example.zerotouchbudget.domain.util.DateUtils
import com.example.zerotouchbudget.presentation.widget.BudgetWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.zerotouchbudget.data.local.AppPreferences
import com.example.zerotouchbudget.data.service.scanner.SmartReceiptScanner

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val summaryRepository: DailySummaryRepository,
    private val appPreferences: AppPreferences,
    private val smartScanner: SmartReceiptScanner,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentBudget = MutableStateFlow(100.0)
    val currentBudget: StateFlow<Double> = _currentBudget.asStateFlow()

    private val _isAutoScanEnabled = MutableStateFlow(appPreferences.isAutoScanEnabled)
    val isAutoScanEnabled: StateFlow<Boolean> = _isAutoScanEnabled.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadCurrentBudget()
    }

    private fun loadCurrentBudget() {
        viewModelScope.launch {
            val today = DateUtils.getCurrentDateString()
            val summary = summaryRepository.getSummaryForDate(today).first()
            if (summary != null) {
                _currentBudget.value = summary.budgetLimit
            }
        }
    }

    fun saveBudget(newBudget: Double) {
        viewModelScope.launch {
            val today = DateUtils.getCurrentDateString()
            summaryRepository.updateBudgetLimit(today, newBudget)
            _currentBudget.value = newBudget
            BudgetWidget().updateAll(context)
        }
    }

    fun toggleAutoScan(enabled: Boolean) {
        _isAutoScanEnabled.value = enabled
        appPreferences.isAutoScanEnabled = enabled
    }

    fun scanExistingImages() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // คำนวณเวลาย้อนหลัง 7 วัน (แปลงเป็น Milliseconds)
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                
                // สั่งสแกนรูปเก่าๆ ทั้งหมด (limit = 200 รูป)
                smartScanner.scan(limit = 200, sinceTimestamp = sevenDaysAgo)
                
                BudgetWidget().updateAll(context)
            } catch (e: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }
}
