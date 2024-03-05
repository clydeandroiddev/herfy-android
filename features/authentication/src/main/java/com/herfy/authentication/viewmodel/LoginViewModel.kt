package com.herfy.authentication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.herfy.authentication.state.LoginViewState
import com.herfy.model.ErrorMsgItem
import com.herfy.repository.AuthRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.launchWithTimber
import com.skylinedynamics.solosdk.api.models.objects.Customer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _showError = MutableLiveData<ErrorMsgItem>()
    val showError: LiveData<ErrorMsgItem> = _showError

    private val _state: MutableSharedFlow<LoginViewState> = MutableSharedFlow()

    val state: Flow<LoginViewState> = _state

    fun login(code: String, phoneNumber: String) {
        invoke {
            if (phoneNumber.isEmpty()) {
                _showError.value = ErrorMsgItem(
                    true, CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED)
                )
            } else {
                viewModelScope.launchWithTimber {
                    _state.emit(LoginViewState.ShowLoading(true))
                }
                val mobile = code + "" + phoneNumber
                authRepository.login(mobile) {
                    viewModelScope.launchWithTimber {
                        _state.emit(LoginViewState.ShowLoading(false))
                        if (it.success) {
                            _state.emit(LoginViewState.UserLogin(it.data as Customer))
                        } else {
                            _showError.value = ErrorMsgItem(true, it.message)
                        }
                    }
                }
            }
        }
    }
}

