package com.herfy.authentication.viewmodel

import androidx.lifecycle.viewModelScope
import com.herfy.authentication.state.LoginOTPPageViewState
import com.herfy.repository.AuthRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DispatcherProvider
import com.herfy.utils.helper.StringUtil
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

    fun verify(code: String, customerId: String) {
        viewModelScope.launchWithTimber(dispatchers.io) {
            if (code.length < 6) {
                _state.emit(
                    LoginOTPPageViewState.ShowError(
                        CacheUtil.getInstance().getTranslations(Text.CODE_REQUIRED)
                    )
                )
            } else {
                _state.emit(LoginOTPPageViewState.ShowLoading(true))
                authRepository.verify(customerId, code) {
                    viewModelScope.launchWithTimber(dispatchers.io) {
                        _state.emit(LoginOTPPageViewState.ShowLoading(false))
                        if (it.success) {
                            getCustomer(customerId)
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

    fun updateName(name: String, customerId: String) {
        viewModelScope.launchWithTimber(dispatchers.io) {
            if (name.isEmpty()) {
                var error = CacheUtil.getInstance().getTranslations(Text.FIRST_NAME_EMPTY)
                if (!StringUtil.isValidName(name)) {
                    error = CacheUtil.getInstance().getTranslations(
                        Text.NAME_VALIDATION,
                        "Name fields should not accept numbers and special character."
                    )
                }
                _state.emit(
                    LoginOTPPageViewState.ShowError(
                        error
                    )
                )
            } else {
                _state.emit(LoginOTPPageViewState.ShowLoading(true))
                authRepository.updateName(customerId, name) {
                    viewModelScope.launchWithTimber(dispatchers.io) {
                        _state.emit(LoginOTPPageViewState.ShowLoading(false))
                        if (it.success) {
                            _state.emit(LoginOTPPageViewState.SuccessUpdate)
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

    fun getCustomer(customerId: String) {
        viewModelScope.launchWithTimber(dispatchers.io) {
            _state.emit(LoginOTPPageViewState.ShowLoading(true))
            authRepository.getCustomer(customerId) {
                viewModelScope.launchWithTimber(dispatchers.io) {
                    _state.emit(LoginOTPPageViewState.ShowLoading(false))
                    if (it.success) {
                        _state.emit(LoginOTPPageViewState.SuccessVerify)
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
