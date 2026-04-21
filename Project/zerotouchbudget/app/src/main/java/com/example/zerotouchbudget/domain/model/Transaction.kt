package com.example.zerotouchbudget.domain.model

enum class TransactionSource {
    NOTIFICATION,
    OCR,
    MANUAL
}

data class Transaction(
    val id: String,
    val amount: Double,
    val brand: String,
    val category: String = "Uncategorized",
    val timestamp: Long,
    val source: TransactionSource,
    val note: String = ""
)