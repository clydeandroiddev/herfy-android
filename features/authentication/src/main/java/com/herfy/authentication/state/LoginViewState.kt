package com.herfy.authentication.state

import com.skylinedynamics.solosdk.api.models.objects.Customer

sealed class LoginViewState {
    data class UserLogin(val customer: Customer) : LoginViewState()

    data class GuestSuccess(val customerId: String, val phoneNumber: String) : LoginViewState()
    data class ShowLoading(val loading: Boolean) : LoginViewState()
    data class ShowError(val error: String): LoginViewState()
}