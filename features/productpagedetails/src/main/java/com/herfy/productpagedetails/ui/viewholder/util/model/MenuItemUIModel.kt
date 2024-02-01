package com.herfy.productpagedetails.ui.viewholder.util.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MenuItemUIModel(
    val data: List<MenuItemDetailsUIModel>
): Parcelable