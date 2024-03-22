package com.herfy.favorites

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FavoriteOrderTypeUIModel(
    val id:String,
    val title:String,
    val orderTypeName:String,
): Parcelable