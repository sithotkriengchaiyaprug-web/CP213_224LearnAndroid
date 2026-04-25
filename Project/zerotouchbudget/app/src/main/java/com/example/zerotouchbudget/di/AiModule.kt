package com.example.zerotouchbudget.di

import com.example.zerotouchbudget.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-2.0-flash", // 1500 RPM free tier vs 20 RPM for 2.5-flash
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }
}