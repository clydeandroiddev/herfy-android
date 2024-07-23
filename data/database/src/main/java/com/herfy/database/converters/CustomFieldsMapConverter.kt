package com.herfy.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CustomFieldsMapConverter {

    @TypeConverter
    fun fromCustomFieldsMap(value: Map<String, Any?>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toCustomFieldsMap(value: String): Map<String, Any?> {
        return Gson().fromJson(value, object : TypeToken<Map<String, Any?>>() {}.type)
    }

}