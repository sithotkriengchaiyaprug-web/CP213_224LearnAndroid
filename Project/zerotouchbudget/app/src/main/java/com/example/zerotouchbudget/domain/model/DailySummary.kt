package com.example.zerotouchbudget.domain.model

data class DailySummary(
    val date: String,
    val budgetLimit: Double,
    val totalSpent: Double,
    val surplus: Double,
    val transactionCount: Int
)