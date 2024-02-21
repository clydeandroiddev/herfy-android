package com.herfy.common.utils

import com.google.android.gms.maps.model.LatLng

fun LatLng.staticMap(apiKey: String): String {
    return "https://maps.google.com/maps/api/staticmap?center=" + this.latitude + "," + this.longitude + "&zoom=15&size=600x300&sensor=false&key=" + apiKey
}
