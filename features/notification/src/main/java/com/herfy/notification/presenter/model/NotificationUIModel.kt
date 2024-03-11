package com.herfy.notification.presenter.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
@Parcelize
data class NotificationUIModel(
    val id: Int,
    val description: String,
    val minutes: String,
    val image: String
) : Parcelable