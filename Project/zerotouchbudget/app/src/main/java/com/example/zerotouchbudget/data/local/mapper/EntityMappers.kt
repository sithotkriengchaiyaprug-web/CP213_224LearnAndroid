package com.example.zerotouchbudget.data.local.mapper

import com.example.zerotouchbudget.data.local.entity.DailySummaryEntity
import com.example.zerotouchbudget.data.local.entity.TransactionEntity
import com.example.zerotouchbudget.domain.model.DailySummary
import com.example.zerotouchbudget.domain.model.Transaction
import com.example.zerotouchbudget.domain.model.TransactionSource

fun TransactionEntity.toDomainModel(): Transaction = Transaction(
    id = id,
    amount = amount,
    brand = brand,
    category = category,
    timestamp = timestamp,
    source = try {
        TransactionSource.valueOf(source)
    } catch (_: IllegalArgumentException) {
        TransactionSource.MANUAL
    },
    note = note
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amount = amount,
    brand = brand,
    category = category,
    timestamp = timestamp,
    source = source.name,
    note = note
)

fun DailySummaryEntity.toDomainModel(): DailySummary = DailySummary(
    date = date,
    budgetLimit = budgetLimit,
    totalSpent = totalSpent,
    surplus = surplus,
    transactionCount = transactionCount
)

fun DailySummary.toEntity(): DailySummaryEntity = DailySummaryEntity(
    date = date,
    budgetLimit = budgetLimit,
    totalSpent = totalSpent,
    surplus = surplus,
    transactionCount = transactionCount
)