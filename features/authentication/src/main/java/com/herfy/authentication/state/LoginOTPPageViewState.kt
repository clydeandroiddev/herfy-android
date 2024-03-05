package com.herfy.authentication.state

sealed class LoginOTPPageViewState {
    object UserResend : LoginOTPPageViewState()
    data class ShowLoading(val loading: Boolean) : LoginOTPPageViewState()
    data class ShowError(val error: String) : LoginOTPPageViewState()
}