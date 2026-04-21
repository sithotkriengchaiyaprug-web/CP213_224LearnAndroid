package com.example.zerotouchbudget.domain.repository

import com.example.zerotouchbudget.domain.model.DailySummary
import kotlinx.coroutines.flow.Flow

interface DailySummaryRepository {
    fun getSummaryForDate(date: String): Flow<DailySummary?>
    suspend fun saveSummary(summary: DailySummary)
    suspend fun getTotalAccumulatedSurplus(): Double
    suspend fun updateBudgetLimit(date: String, newLimit: Double)
    fun getSummariesInRange(startDate: String, endDate: String): Flow<List<DailySummary>>
}