package com.herfy.di

import androidx.fragment.app.Fragment
import com.herfy.cart.CartContract
import com.herfy.cart.CartPresenter
import com.herfy.cart.views.CartFragment
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



}

@InstallIn(FragmentComponent::class)
@Module
object FragmentModule {

    @Provides
    fun providesCartFragment(fragment: Fragment): CartFragment {
        return fragment as CartFragment
    }
}