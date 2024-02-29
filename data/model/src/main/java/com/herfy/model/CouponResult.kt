package com.herfy.model

import com.skylinedynamics.solosdk.api.models.objects.Campaign

data class CouponResult (
    val isSuccess : Boolean,
    val promo : Campaign?,
    val message : String,
    val removeCoupon : Boolean = false
)