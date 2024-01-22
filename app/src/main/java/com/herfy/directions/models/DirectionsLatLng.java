package com.herfy.directions.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class DirectionsLatLng implements Serializable {

    @SerializedName("lat")
    private double latitude = 0.0;

    @SerializedName("lng")
    private double longitude = 0.0;

    public DirectionsLatLng() {

    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

}
