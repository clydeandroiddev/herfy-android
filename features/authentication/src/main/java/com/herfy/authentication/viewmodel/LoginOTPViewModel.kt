package com.herfy.authentication.viewmodel

import androidx.lifecycle.viewModelScope
import com.herfy.authentication.state.LoginOTPPageViewState
import com.herfy.repository.AuthRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.helper.DispatcherProvider
import com.herfy.utils.helper.launchWithTimber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class LoginOTPViewModel @Inject constructor(
    private val authRepository: AuthRepository, private val dispatchers: DispatcherProvider
) : BaseViewModel() {

    private val _state: MutableSharedFlow<LoginOTPPageViewState> = MutableSharedFlow()

    val state: Flow<LoginOTPPageViewState> = _state
    fun resend(customerId: String, phoneNumber: String) {
        viewModelScope.launchWithTimber(dispatchers.io) {
            _state.emit(LoginOTPPageViewState.ShowLoading(true))
            authRepository.sendVerification(customerId, phoneNumber) {
                viewModelScope.launchWithTimber(dispatchers.io) {
                    _state.emit(LoginOTPPageViewState.ShowLoading(false))
                    if (it.success) {
                        _state.emit(LoginOTPPageViewState.UserResend)
                    } else {
                        _state.emit(
                            LoginOTPPageViewState.ShowError(
                                it.message
                            )
                        )
                    }
                }
            }
        }
    }
}
