package com.herfy.phonenumber.model


sealed class EnterNewPhoneNumberState {

    data class IsLoading(val isLoading:Boolean) : EnterNewPhoneNumberState()

    data class Error(val message:String) : EnterNewPhoneNumberState()

    data class Success(val phoneNumber:String): EnterNewPhoneNumberState()
}