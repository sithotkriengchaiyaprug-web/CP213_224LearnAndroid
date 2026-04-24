package com.example.zerotouchbudget.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.zerotouchbudget.data.local.dao.DailySummaryDao
import com.example.zerotouchbudget.data.local.dao.TransactionDao
import com.example.zerotouchbudget.data.local.entity.DailySummaryEntity
import com.example.zerotouchbudget.data.local.entity.TransactionEntity

import com.example.zerotouchbudget.data.local.dao.ProcessedReceiptDao
import com.example.zerotouchbudget.data.local.entity.ProcessedReceiptEntity

@Database(
    entities = [
        TransactionEntity::class,
        DailySummaryEntity::class,
        ProcessedReceiptEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun processedReceiptDao(): ProcessedReceiptDao
}