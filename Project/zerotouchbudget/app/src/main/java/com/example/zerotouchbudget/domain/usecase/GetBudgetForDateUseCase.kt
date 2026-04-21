package com.example.zerotouchbudget.domain.usecase

import com.example.zerotouchbudget.domain.repository.DailySummaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * ดึงค่าขีดจำกัดงบประมาณรายวัน (Budget Limit) สำหรับวันที่ระบุ
 * หากไม่มีการตั้งค่าสำหรับวันนั้น จะคืนค่า Default (เช่น 100.0)
 */
class GetBudgetForDateUseCase @Inject constructor(
    private val repository: DailySummaryRepository
) {
    private val DEFAULT_BUDGET = 300.0 // สามารถเปลี่ยนเป็นดึงจาก Preferences ภายหลังได้

    operator fun invoke(dateString: String): Flow<Double> {
        return repository.getSummaryForDate(dateString).map { summary ->
            summary?.budgetLimit ?: DEFAULT_BUDGET
        }
    }
}