package com.herfy.di

import com.herfy.utils.helper.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DispatchersModule {

    @Provides
    @Singleton
    fun providesDispatcherSource(): DispatcherProvider =
        object : DispatcherProvider {
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val io: CoroutineDispatcher = Dispatchers.IO
            override val ui: CoroutineDispatcher = Dispatchers.Main
            override val uiImmediate: CoroutineDispatcher = Dispatchers.Main.immediate
        }

}