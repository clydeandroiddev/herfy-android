package com.herfy.di

import android.content.Context
import com.herfy.database.AppDatabase
import com.herfy.utils.helper.CacheUtilHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HelperModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context : Context) = AppDatabase.initWith(context)

    @Provides
    @Singleton
    fun provideCacheUtilHelper(@ApplicationContext context: Context) = CacheUtilHelper(context)
}