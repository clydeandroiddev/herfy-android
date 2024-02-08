package com.herfy.model

import com.skylinedynamics.solosdk.api.models.objects.OrderType

data class PromotionCategory(
    val name : String,
    var isSelected : Boolean = false,
    val orderType: OrderType
)
