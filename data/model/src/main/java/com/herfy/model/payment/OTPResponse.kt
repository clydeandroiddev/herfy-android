package com.herfy.model.payment

import com.herfy.model.ErrorMsgItem

data class OTPResponse(
    val isSuccess: Boolean,
    val data : OTPData? = null,
    val error: ErrorMsgItem?
)
