package com.example.zerotouchbudget.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.zerotouchbudget.data.local.dao.DailySummaryDao
import com.example.zerotouchbudget.data.local.dao.TransactionDao
import com.example.zerotouchbudget.data.local.entity.DailySummaryEntity
import com.example.zerotouchbudget.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        DailySummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun dailySummaryDao(): DailySummaryDao
}