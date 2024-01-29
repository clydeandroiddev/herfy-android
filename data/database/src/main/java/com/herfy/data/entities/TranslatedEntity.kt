package com.herfy.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import com.skylinedynamics.solosdk.api.models.objects.Translated
import kotlinx.parcelize.Parcelize

@Parcelize
data class TranslatedEntity(
    @ColumnInfo(name = "ar_sa") @SerializedName("ar-sa") var arabic: String? = "",
    @ColumnInfo(name = "en_us") @SerializedName("en-us") var english: String? = "",
    @ColumnInfo(name = "fr") @SerializedName("fr") var french: String? = "",
) : Parcelable

fun TranslatedEntity?.toDomain(): Translated {
    this?.let {
        val translated = Translated()
        translated.arabic = if(arabic.isNullOrEmpty()) english else arabic
        translated.english = english
        translated.french = if(french.isNullOrEmpty()) english else french
        return translated
    }
    return Translated()
}