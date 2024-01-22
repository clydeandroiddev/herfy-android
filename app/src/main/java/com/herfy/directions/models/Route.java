package com.herfy.directions.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class Route implements Serializable {

    @SerializedName("bounds")
    private Bounds bounds =  new Bounds();

    @SerializedName("legs")
    private ArrayList<Leg> legs = new ArrayList<>();

    @SerializedName("overview_polyline")
    private OverviewPolyline overviewPolyline =  new OverviewPolyline();

    public Route() {

    }

    public Bounds getBounds() {
        return bounds;
    }

    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
    }

    public ArrayList<Leg> getLegs() {
        return legs;
    }

    public void setLegs(ArrayList<Leg> legs) {
        this.legs = legs;
    }

    public OverviewPolyline getOverviewPolyline() {
        return overviewPolyline;
    }

    public void setOverviewPolyline(OverviewPolyline overviewPolyline) {
        this.overviewPolyline = overviewPolyline;
    }

}
