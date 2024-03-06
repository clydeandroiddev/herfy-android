package com.herfy.model.auth

data class GuestResponse(
    val success: Boolean,
    val customerId: String,
    val message: String = ""
)
