package com.example.zerotouchbudget.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.zerotouchbudget.data.local.BudgetDatabase
import com.example.zerotouchbudget.data.local.dao.DailySummaryDao
import com.example.zerotouchbudget.data.local.dao.ProcessedReceiptImageDao
import com.example.zerotouchbudget.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): BudgetDatabase {
        return Room.databaseBuilder(
            context,
            BudgetDatabase::class.java,
            "budget_database"
        ).addMigrations(MIGRATION_1_2)
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
    fun provideProcessedReceiptImageDao(db: BudgetDatabase): ProcessedReceiptImageDao =
        db.processedReceiptImageDao()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `processed_receipt_images` (
                    `imageUri` TEXT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `relativePath` TEXT NOT NULL,
                    `folderName` TEXT NOT NULL,
                    `processedAtMillis` INTEGER NOT NULL,
                    `ocrPassed` INTEGER NOT NULL,
                    `aiProcessed` INTEGER NOT NULL,
                    PRIMARY KEY(`imageUri`)
                )
                """.trimIndent()
            )
        }
    }
}
