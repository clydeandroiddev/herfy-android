package com.herfy.authentication.state

sealed class LoginOTPPageViewState {
    object UserResend : LoginOTPPageViewState()

    object SuccessVerify : LoginOTPPageViewState()

    object SuccessUpdate : LoginOTPPageViewState()
    data class ShowLoading(val loading: Boolean) : LoginOTPPageViewState()
    data class ShowError(val error: String) : LoginOTPPageViewState()
}