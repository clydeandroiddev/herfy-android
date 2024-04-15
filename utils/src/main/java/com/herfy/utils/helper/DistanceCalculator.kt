package com.herfy.utils.helper
import android.location.Location

object DistanceCalculator {

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {

        val result = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)

        return result[0].toDouble()
    }

}