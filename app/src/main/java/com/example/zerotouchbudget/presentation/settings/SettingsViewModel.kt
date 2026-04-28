package com.example.zerotouchbudget.presentation.settings

import android.content.Context
import com.example.zerotouchbudget.data.service.ReceiptAutoScanScheduler
import com.example.zerotouchbudget.domain.model.AutoScanSettings
import com.example.zerotouchbudget.domain.repository.AutoScanSettingsRepository
import com.example.zerotouchbudget.domain.repository.DailySummaryRepository
import com.example.zerotouchbudget.domain.util.DateUtils
import com.example.zerotouchbudget.presentation.widget.BudgetWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val summaryRepository: DailySummaryRepository,
    private val autoScanSettingsRepository: AutoScanSettingsRepository,
    private val autoScanScheduler: ReceiptAutoScanScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentBudget = MutableStateFlow(100.0)
    val currentBudget: StateFlow<Double> = _currentBudget.asStateFlow()

    val autoScanSettings: StateFlow<AutoScanSettings> = autoScanSettingsRepository.settings
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AutoScanSettings()
        )

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

    fun saveAutoScanSettings(settings: AutoScanSettings) {
        viewModelScope.launch {
            autoScanSettingsRepository.saveSettings(settings)
            if (settings.enabled) {
                autoScanScheduler.schedule(settings)
            } else {
                autoScanScheduler.cancel()
            }
        }
    }

    fun syncHistory() {
        viewModelScope.launch {
            autoScanScheduler.syncHistory()
        }
    }
}

