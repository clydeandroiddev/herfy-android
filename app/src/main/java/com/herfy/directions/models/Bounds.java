package com.herfy.directions.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Bounds implements Serializable {

    @SerializedName("northeast")
    private DirectionsLatLng northeast = new DirectionsLatLng();

    @SerializedName("southwest")
    private DirectionsLatLng southwest = new DirectionsLatLng();

    public Bounds() {

    }

    public DirectionsLatLng getNortheast() {
        return northeast;
    }

    public void setNortheast(DirectionsLatLng northeast) {
        this.northeast = northeast;
    }

    public DirectionsLatLng getSouthwest() {
        return southwest;
    }

    public void setSouthwest(DirectionsLatLng southwest) {
        this.southwest = southwest;
    }

}
