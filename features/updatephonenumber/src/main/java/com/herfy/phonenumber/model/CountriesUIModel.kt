package com.herfy.phonenumber.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CountriesUIModel(
    val flag : String,
    val dialCode : String
): Parcelable