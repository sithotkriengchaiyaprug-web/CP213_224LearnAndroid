package com.example.zerotouchbudget.data.repository

import com.example.zerotouchbudget.data.local.dao.DailySummaryDao
import com.example.zerotouchbudget.data.local.mapper.toDomainModel
import com.example.zerotouchbudget.data.local.mapper.toEntity
import com.example.zerotouchbudget.domain.model.DailySummary
import com.example.zerotouchbudget.domain.repository.DailySummaryRepository
import kotlinx.coroutines.flow.Flow
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
}