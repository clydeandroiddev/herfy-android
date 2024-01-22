package com.herfy.directions.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Leg implements Serializable {

    @SerializedName("start_location")
    private DirectionsLatLng startLocation = new DirectionsLatLng();

    @SerializedName("start_address")
    private String startAddress = "";

    @SerializedName("end_location")
    private DirectionsLatLng endLocation = new DirectionsLatLng();

    @SerializedName("end_address")
    private String endAddress = "";

    @SerializedName("overview_polyline")
    private OverviewPolyline overviewPolyline =  new OverviewPolyline();

    public Leg() {

    }

    public DirectionsLatLng getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(DirectionsLatLng startLocation) {
        this.startLocation = startLocation;
    }

    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public DirectionsLatLng getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(DirectionsLatLng endLocation) {
        this.endLocation = endLocation;
    }

    public String getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(String endAddress) {
        this.endAddress = endAddress;
    }

    public OverviewPolyline getOverviewPolyline() {
        return overviewPolyline;
    }

    public void setOverviewPolyline(OverviewPolyline overviewPolyline) {
        this.overviewPolyline = overviewPolyline;
    }

}
