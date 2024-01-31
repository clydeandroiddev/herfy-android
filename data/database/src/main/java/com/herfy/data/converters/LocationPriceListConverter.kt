package com.herfy.data.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylinedynamics.solosdk.api.models.objects.LocationPrice

class LocationPriceListConverter {

    @TypeConverter
    fun fromLocationPriceList(value: List<LocationPrice>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toLocationPriceList(value: String): List<LocationPrice> {
        return Gson().fromJson(value, object : TypeToken<List<LocationPrice>>() {}.type)
    }

}