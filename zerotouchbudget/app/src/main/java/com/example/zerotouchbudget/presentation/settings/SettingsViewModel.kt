package com.example.zerotouchbudget.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zerotouchbudget.domain.repository.DailySummaryRepository
import com.example.zerotouchbudget.domain.util.DateUtils
import com.example.zerotouchbudget.presentation.widget.BudgetWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val summaryRepository: DailySummaryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentBudget = MutableStateFlow(100.0)
    val currentBudget: StateFlow<Double> = _currentBudget.asStateFlow()

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
}