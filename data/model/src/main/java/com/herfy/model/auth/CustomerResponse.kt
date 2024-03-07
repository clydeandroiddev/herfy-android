package com.herfy.model.auth

data class CustomerResponse(
    val success: Boolean,
    val data: Any?,
    val message: String = ""
)
