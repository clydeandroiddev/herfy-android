package com.herfy.di

import com.herfy.database.AppDatabase
import com.herfy.database.repositories.FavoriteRepository
import com.herfy.database.repositories.FavoriteRepositoryImpl
import com.herfy.database.repositories.MenuRepository
import com.herfy.database.repositories.MenuRepositoryImpl
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

    @Provides
    @Singleton
    fun provideMenuRepository(
        appDatabase: AppDatabase,
        cacheUtilHelper: CacheUtilHelper
    ) : MenuRepository = MenuRepositoryImpl(appDatabase, cacheUtilHelper)

    @Provides
    @Singleton
    fun provideFavoriteRepository(
        appDatabase: AppDatabase,
        cacheUtilHelper: CacheUtilHelper
    ) : FavoriteRepository = FavoriteRepositoryImpl()




}