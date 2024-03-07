package com.herfy.di

import android.app.Activity
import com.herfy.loyalty.LoyaltyActivity
import com.herfy.loyalty.LoyaltyContract
import com.herfy.loyalty.LoyaltyPresenter
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@InstallIn(ActivityComponent::class)
@Module
abstract class ActivityPresenterModule {
    @Binds
    abstract fun bindLoyaltyActivity(impl: LoyaltyActivity): LoyaltyContract.View
    @Binds
    abstract fun bindLoyaltyPresenter(impl: LoyaltyPresenter): LoyaltyContract.Presenter
}

@InstallIn(ActivityComponent::class)
@Module
object ActivityModule {
    @Provides
    fun providesLoyaltyActivity(activity: Activity): LoyaltyActivity {
        return activity as LoyaltyActivity
    }
}