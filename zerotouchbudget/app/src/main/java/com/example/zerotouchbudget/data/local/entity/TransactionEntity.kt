package com.example.zerotouchbudget.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["source"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val amount: Double,
    val brand: String,
    val category: String,
    val timestamp: Long,
    val source: String,
    val note: String
)