package com.herfy.productpagedetails.ui.viewholder.util.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalPriceUIModel(
    val location: Int,
    val price: Double,
    val menuTemplateCode:String
): Parcelable