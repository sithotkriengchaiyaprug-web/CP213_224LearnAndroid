package com.example.zerotouchbudget.data.repository

import com.example.zerotouchbudget.data.local.dao.DailySummaryDao
import com.example.zerotouchbudget.data.local.entity.DailySummaryEntity
import com.example.zerotouchbudget.data.local.mapper.toDomainModel
import com.example.zerotouchbudget.data.local.mapper.toEntity
import com.example.zerotouchbudget.domain.model.DailySummary
import com.example.zerotouchbudget.domain.repository.DailySummaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DailySummaryRepositoryImpl @Inject constructor(
    private val dailySummaryDao: DailySummaryDao
) : DailySummaryRepository {

    override fun getSummaryForDate(date: String): Flow<DailySummary?> {
        return dailySummaryDao.getSummaryForDate(date)
            .map { it?.toDomainModel() }
    }

    override suspend fun saveSummary(summary: DailySummary) {
        dailySummaryDao.insertOrUpdate(summary.toEntity())
    }

    override suspend fun getTotalAccumulatedSurplus(): Double {
        return dailySummaryDao.getTotalAccumulatedSurplus()
    }

    override suspend fun updateBudgetLimit(date: String, newLimit: Double) {
        val existing = dailySummaryDao.getSummaryForDate(date).first()
        if (existing != null) {
            val updated = existing.copy(budgetLimit = newLimit)
            dailySummaryDao.insertOrUpdate(updated)
        } else {
            val newSummary = DailySummaryEntity(
                date = date,
                budgetLimit = newLimit,
                totalSpent = 0.0,
                surplus = newLimit,
                transactionCount = 0
            )
            dailySummaryDao.insertOrUpdate(newSummary)
        }
    }
}