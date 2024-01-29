package com.herfy.di

import com.herfy.database.AppDatabase
import com.herfy.home.repository.HomeRepository
import com.herfy.home.repository.HomeRepositoryImpl
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
        appDatabase: AppDatabase
    ) : HomeRepository = HomeRepositoryImpl(appDatabase)


}