package com.herfy.popup

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Popup(
    @SerializedName("action-type") val actionType: String?,
    @SerializedName("action-value") val actionValue: String?,
    @SerializedName("button-label") val buttonLabel: String?,
    @SerializedName("label") val label: String?,
    @SerializedName("image-uri") val imageUri: String?
): Parcelable
