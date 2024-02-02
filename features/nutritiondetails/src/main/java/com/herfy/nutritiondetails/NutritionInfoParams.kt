package com.herfy.nutritiondetails

import android.os.Parcelable
import androidx.core.text.PrecomputedTextCompat.Params
import kotlinx.parcelize.Parcelize

@Parcelize
data class NutritionInfoParams(
    val nutritionImageUrl: String,
    val nutritionStory: String,
    val menuName:String,
) : Parcelable