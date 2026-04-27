package com.example.zerotouchbudget.di

import com.example.zerotouchbudget.data.local.AppPreferences
import com.example.zerotouchbudget.domain.repository.DailySummaryRepository
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun transactionRepository(): TransactionRepository
    fun appPreferences(): AppPreferences
    fun dailySummaryRepository(): DailySummaryRepository
}