package com.herfy.di

import com.herfy.database.AppDatabase
import com.herfy.repository.HomeRepository
import com.herfy.repository.HomeRepositoryImpl
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
    ) : HomeRepository =
       HomeRepositoryImpl(appDatabase, cacheUtilHelper)


}