package com.herfy.authentication.state

import com.skylinedynamics.solosdk.api.models.objects.Customer

sealed class LoginViewState {
    data class UserLogin(val customer: Customer) : LoginViewState()
    data class ShowLoading(val loading: Boolean) : LoginViewState()
}