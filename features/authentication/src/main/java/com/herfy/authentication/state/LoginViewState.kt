package com.herfy.authentication.state

import com.skylinedynamics.solosdk.api.models.objects.Customer

sealed class LoginViewState {
    data class UserLogin(val customer: Customer) : LoginViewState()

    data class GuestSuccess(val customerId: String, val phoneNumber: String) : LoginViewState()

    data class GoogleSuccess(val customer: Customer) : LoginViewState()

    data class GoogleSuccessFromAddNumber(val customer: Customer, val mobile: String) :
        LoginViewState()

    data class ShowAddNumber(val customer: Customer) : LoginViewState()

    data class GoogleGoOTP(val customer: Customer) : LoginViewState()
    data class ShowLoading(val loading: Boolean) : LoginViewState()
    data class ShowError(val error: String) : LoginViewState()
}