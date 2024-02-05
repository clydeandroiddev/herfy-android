package com.herfy.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class SupportedCountryResponse {
    @SerializedName("data")
    public List<SupportedCountry> data = new ArrayList<>();
}
