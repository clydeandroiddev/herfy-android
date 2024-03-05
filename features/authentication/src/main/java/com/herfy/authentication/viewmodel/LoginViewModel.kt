package com.herfy.authentication.viewmodel

import android.util.Log
import com.herfy.repository.AuthRepository
import com.herfy.repository.FavoriteRepository
import com.herfy.repository.HomeRepository
import com.herfy.repository.MenuRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    fun login(phoneNumber: String) {
        invoke {
            authRepository.login(phoneNumber) {
                Log.e("==AAASD", "ADADADADSuccess:")
            }
        }
    }

}

