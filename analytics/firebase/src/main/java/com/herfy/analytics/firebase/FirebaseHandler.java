package com.herfy.analytics.firebase;

import com.google.gson.JsonObject;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

public interface FirebaseHandler {

    @POST("fcm/send")
    Call<ResponseBody> sendNotification(@Body JsonObject body, @HeaderMap Map<String, String> headers);

}

