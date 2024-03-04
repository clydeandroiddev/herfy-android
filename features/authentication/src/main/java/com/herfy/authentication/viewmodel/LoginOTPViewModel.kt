package com.herfy.authentication.viewmodel

import com.herfy.repository.FavoriteRepository
import com.herfy.repository.HomeRepository
import com.herfy.repository.MenuRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginOTPViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val menuRepository: MenuRepository,
    private val favoriteRepository: FavoriteRepository
) : BaseViewModel() {

}
