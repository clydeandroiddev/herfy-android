package com.herfy.di

import com.herfy.data.AppDatabase
import com.herfy.home.repository.HomeRepository
import com.herfy.home.repository.HomeRepositoryImpl
import com.herfy.utils.helper.CacheUtilHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideHomeRepository(
        appDatabase: AppDatabase,
        cacheUtilHelper: CacheUtilHelper
    ) : HomeRepository = HomeRepositoryImpl(appDatabase, cacheUtilHelper)


}