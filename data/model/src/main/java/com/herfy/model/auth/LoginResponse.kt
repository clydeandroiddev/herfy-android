package com.herfy.model.auth

data class LoginResponse(
    val success: Boolean,
    val showCode: Boolean,
    val data: Any?,
    val message: String = ""
)
