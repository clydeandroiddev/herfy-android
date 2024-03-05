package com.herfy.model

data class LoginResponse(
    val success: Boolean,
    val showCode: Boolean,
    val data: Any?,
    val message: String = ""
)
