package com.herfy.checkout

sealed class DialogRequestOTPState {

    data class IsLoading(val isLoading: Boolean) : DialogRequestOTPState()

    data class Error(val message:String) : DialogRequestOTPState()

    object Success: DialogRequestOTPState()
}