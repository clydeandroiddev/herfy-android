package com.herfy.directions.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class DirectionsResponse implements Serializable {

    @SerializedName("status")
    private String status = "";

    @SerializedName("routes")
    private ArrayList<Route> routes = new ArrayList<>();

    public DirectionsResponse() {

    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ArrayList<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(ArrayList<Route> routes) {
        this.routes = routes;
    }

}
