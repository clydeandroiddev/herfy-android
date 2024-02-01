package com.herfy.productpagedetails.ui.viewholder.util.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MenuItemDetailsUIModel(
    val type: String,
    val id: String,
    val attributesUIModel: ItemAttributesUIModel
): Parcelable