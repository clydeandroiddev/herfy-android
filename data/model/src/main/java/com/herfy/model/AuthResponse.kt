package com.herfy.model

data class AuthResponse(
    val success: Boolean,
    val data: Any?,
    val message: String = ""
)
