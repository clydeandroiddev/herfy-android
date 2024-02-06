package com.herfy.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.herfy.database.entities.TranslatedEntity

class TranslatedEntityConverter {

    @TypeConverter
    fun fromTranslated(value: TranslatedEntity): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toTranslated(value: String): TranslatedEntity {
        return Gson().fromJson(value, TranslatedEntity::class.java)
    }

}