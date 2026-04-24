package com.example.zerotouchbudget.di

import android.content.Context
import androidx.room.Room
import com.example.zerotouchbudget.data.local.BudgetDatabase
import com.example.zerotouchbudget.data.local.dao.DailySummaryDao
import com.example.zerotouchbudget.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): BudgetDatabase {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `processed_receipts` (`imageUri` TEXT NOT NULL, `dateAdded` INTEGER NOT NULL, `fileSize` INTEGER NOT NULL, `processedAt` INTEGER NOT NULL, PRIMARY KEY(`imageUri`))"
                )
            }
        }

        return Room.databaseBuilder(
            context,
            BudgetDatabase::class.java,
            "budget_database"
        )
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(db: BudgetDatabase): TransactionDao = db.transactionDao()

    @Provides
    @Singleton
    fun provideDailySummaryDao(db: BudgetDatabase): DailySummaryDao = db.dailySummaryDao()

    @Provides
    @Singleton
    fun provideProcessedReceiptDao(db: BudgetDatabase): com.example.zerotouchbudget.data.local.dao.ProcessedReceiptDao = db.processedReceiptDao()
}