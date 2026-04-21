package com.example.zerotouchbudget.domain.usecase

import com.example.zerotouchbudget.domain.model.DailySummary
import com.example.zerotouchbudget.domain.repository.DailySummaryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDailySummaryUseCase @Inject constructor(
    private val repository: DailySummaryRepository
) {
    operator fun invoke(dateString: String): Flow<DailySummary?> {
        return repository.getSummaryForDate(dateString)
    }
}