package com.herfy.authentication.viewmodel

import androidx.lifecycle.viewModelScope
import com.herfy.authentication.state.LoginViewState
import com.herfy.repository.AuthRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DispatcherProvider
import com.herfy.utils.helper.launchWithTimber
import com.skylinedynamics.solosdk.api.models.objects.Customer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository, private val dispatchers: DispatcherProvider
) : BaseViewModel() {

    private val _state: MutableSharedFlow<LoginViewState> = MutableSharedFlow()

    val state: Flow<LoginViewState> = _state

    fun login(code: String, phoneNumber: String) {
        viewModelScope.launchWithTimber(dispatchers.io) {
            if (phoneNumber.isEmpty()) {
                _state.emit(
                    LoginViewState.ShowError(
                        CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED)
                    )
                )
            } else {
                _state.emit(LoginViewState.ShowLoading(true))
                val mobile = code + "" + phoneNumber
                processLogin(mobile)
            }
        }
    }

    private suspend fun processLogin(mobile: String) {
        authRepository.login(mobile) {
            viewModelScope.launchWithTimber(dispatchers.io) {
                _state.emit(LoginViewState.ShowLoading(false))
                if (it.success) {
                    _state.emit(LoginViewState.UserLogin(it.data as Customer))
                } else {
                    _state.emit(
                        LoginViewState.ShowError(
                            it.message
                        )
                    )
                }
            }
        }
    }
}

