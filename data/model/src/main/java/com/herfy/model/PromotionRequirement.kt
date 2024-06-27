package com.herfy.model

import androidx.annotation.DrawableRes

data class PromotionRequirement(
    @DrawableRes val icon: Int,
    val label : String,
    val isRequired : Boolean
)
