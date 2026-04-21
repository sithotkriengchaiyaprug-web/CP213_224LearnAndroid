package com.example.zerotouchbudget.domain.usecase

import com.example.zerotouchbudget.domain.repository.DailySummaryRepository
import javax.inject.Inject

/**
 * ตั้งค่าหรืออัปเดตงบประมาณรายวันสำหรับวันที่ระบุ
 */
class SetDailyBudgetUseCase @Inject constructor(
    private val repository: DailySummaryRepository
) {
    suspend operator fun invoke(dateString: String, amount: Double) {
        if (amount < 0) return
        repository.updateBudgetLimit(dateString, amount)
    }
}