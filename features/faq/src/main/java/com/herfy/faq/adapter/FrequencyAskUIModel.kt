package com.herfy.faq.adapter

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FrequencyAskUIModel(
    val frequencyTitle:String,
    val id: Long,
    val url:String,
) : Parcelable