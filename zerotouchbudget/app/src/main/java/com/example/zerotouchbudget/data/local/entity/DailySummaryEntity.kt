package com.example.zerotouchbudget.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey
    val date: String,
    val budgetLimit: Double,
    val totalSpent: Double,
    val surplus: Double,
    val transactionCount: Int
)