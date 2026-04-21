package com.example.zerotouchbudget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zerotouchbudget.data.local.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {

    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    fun getSummaryForDate(date: String): Flow<DailySummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC LIMIT :limit")
    fun getRecentSummaries(limit: Int = 30): Flow<List<DailySummaryEntity>>

    @Query("SELECT COALESCE(SUM(surplus), 0.0) FROM daily_summaries")
    suspend fun getTotalAccumulatedSurplus(): Double

    @Query("SELECT * FROM daily_summaries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getSummariesInRange(startDate: String, endDate: String): Flow<List<DailySummaryEntity>>
}