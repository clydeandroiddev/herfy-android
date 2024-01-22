package com.herfy.promotions.data

import com.skylinedynamics.solosdk.api.models.objects.MenuItem

data class CouponToAdd(
    val couponCode: String? = null,
    val couponName: String? = null,
    val menuItem: MenuItem? = null
)