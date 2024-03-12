package com.herfy.di

import android.content.Context
import com.herfy.database.AppDatabase
import com.herfy.repository.AuthRepository
import com.herfy.repository.AuthRepositoryImpl
import com.herfy.repository.FavoriteRepository
import com.herfy.repository.FavoriteRepositoryImpl
import com.herfy.repository.HomeRepository
import com.herfy.repository.HomeRepositoryImpl
import com.herfy.repository.MenuRepository
import com.herfy.repository.MenuRepositoryImpl
import com.herfy.repository.PaymentRepository
import com.herfy.repository.PaymentRepositoryImpl
import com.herfy.repository.StoreRepository
import com.herfy.repository.StoreRepositoryImpl
import com.herfy.utils.helper.CacheUtilHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    ): HomeRepository =
        HomeRepositoryImpl(appDatabase, cacheUtilHelper)

    @Provides
    @Singleton
    fun provideMenuRepository(
        appDatabase: AppDatabase,
        cacheUtilHelper: CacheUtilHelper,
        @ApplicationContext context : Context
    ): MenuRepository =
        MenuRepositoryImpl(appDatabase, cacheUtilHelper, context)

    @Provides
    @Singleton
    fun provideFavoriteRepository(
        appDatabase: AppDatabase,
        cacheUtilHelper: CacheUtilHelper
    ): FavoriteRepository =
        FavoriteRepositoryImpl()


    @Provides
    @Singleton
    fun provideAuthRepository(
        appDatabase: AppDatabase,
        cacheUtilHelper: CacheUtilHelper
    ): AuthRepository =
        AuthRepositoryImpl()

    @Provides
    @Singleton
    fun provideStoreRepository(
        appDatabase: AppDatabase,
        cacheUtilHelper: CacheUtilHelper
    ): StoreRepository =
        StoreRepositoryImpl()

    @Provides
    @Singleton
    fun providePaymentRepository(
        appDatabase: AppDatabase,
        cacheUtilHelper: CacheUtilHelper
    ) : PaymentRepository = PaymentRepositoryImpl(appDatabase, cacheUtilHelper)


}