package com.herfy.directions.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class OverviewPolyline implements Serializable {

    @SerializedName("points")
    private String points = "";

    public OverviewPolyline() {

    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }

}
