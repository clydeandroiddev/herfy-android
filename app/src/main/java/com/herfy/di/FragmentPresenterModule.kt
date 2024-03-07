package com.herfy.di

import androidx.fragment.app.Fragment
import com.herfy.cart.CartContract
import com.herfy.cart.CartPresenter
import com.herfy.cart.views.CartFragment
import com.herfy.promotions.PromotionsContract
import com.herfy.promotions.PromotionsPresenter
import com.herfy.promotions.views.PromotionsFragment
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent


@InstallIn(FragmentComponent::class)
@Module
abstract class FragmentPresenterModule {

    @Binds
    abstract fun bindCartFragment(impl: CartFragment): CartContract.View


    @Binds
    abstract fun bindCartPresenter(impl: CartPresenter): CartContract.Presenter

    @Binds
    abstract fun bindPromotionsFragment(impl: PromotionsFragment) : PromotionsContract.View

    @Binds
    abstract fun bindPromotionsPresenter(impl : PromotionsPresenter) : PromotionsContract.Presenter



}

@InstallIn(FragmentComponent::class)
@Module
object FragmentModule {

    @Provides
    fun providesCartFragment(fragment: Fragment): CartFragment {
        return fragment as CartFragment
    }

    @Provides
    fun providesPromotionsFragment(fragment : Fragment) : PromotionsFragment{
        return fragment as PromotionsFragment
    }
}