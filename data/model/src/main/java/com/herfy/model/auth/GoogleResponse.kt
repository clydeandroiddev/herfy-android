package com.herfy.model.auth

data class GoogleResponse(
    val success: Boolean,
    val showCode: Boolean,
    val showAdd: Boolean,
    val data: Any?,
    val message: String = ""
)
