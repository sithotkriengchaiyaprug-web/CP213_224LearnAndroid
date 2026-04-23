package com.example.zerotouchbudget.di

import com.example.zerotouchbudget.data.repository.DailySummaryRepositoryImpl
import com.example.zerotouchbudget.data.repository.AutoScanSettingsRepositoryImpl
import com.example.zerotouchbudget.data.repository.ProcessedReceiptImageRepositoryImpl
import com.example.zerotouchbudget.data.repository.TransactionRepositoryImpl
import com.example.zerotouchbudget.domain.repository.AutoScanSettingsRepository
import com.example.zerotouchbudget.domain.repository.DailySummaryRepository
import com.example.zerotouchbudget.domain.repository.ProcessedReceiptImageRepository
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindDailySummaryRepository(
        impl: DailySummaryRepositoryImpl
    ): DailySummaryRepository

    @Binds
    @Singleton
    abstract fun bindAutoScanSettingsRepository(
        impl: AutoScanSettingsRepositoryImpl
    ): AutoScanSettingsRepository

    @Binds
    @Singleton
    abstract fun bindProcessedReceiptImageRepository(
        impl: ProcessedReceiptImageRepositoryImpl
    ): ProcessedReceiptImageRepository
}
