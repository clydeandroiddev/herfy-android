package com.herfy.analytics.firebase;

import com.google.gson.JsonObject;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.util.AppLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirebaseApiCall {

    private FirebaseHandler handler;

    private FirebaseApiCall() {
        handler = FirebaseApiConnector.getInstance().createService(FirebaseHandler.class);
    }

    public static FirebaseApiCall getInstance() {
        return new FirebaseApiCall();
    }

    public void sendNotification(String iosDriverFcmToken, String title, String body, final ApiRequestListener listener) {

        JsonObject notificationObject = new JsonObject();
        notificationObject.addProperty("title", title);
        notificationObject.addProperty("body", body);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("to", iosDriverFcmToken);
        jsonObject.addProperty("priority", "high");
        jsonObject.addProperty("direct_book_ok", true);
        jsonObject.add("notification", notificationObject);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "key=AAAA6TMOx7w:APA91bHnbfCUAbFZwsgU_gqAPRGb2s7UzYQVWVPNnFXqudYltHeHLrmdYyoYyvT2X6RTWIDvFcWjGKMu0QCRyu1rOtiEPEdQP7Ssynz1kmF0CWVCt1j-DIxzA1cgxdpViF-U5Z-G6Z9j");
        Call<ResponseBody> call = handler.sendNotification(jsonObject, headers);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        if(response.body() != null) {
                            String serverResponse = response.body().string();
                            if(serverResponse.length() > 0 && String.valueOf(serverResponse.charAt(0)).equalsIgnoreCase("[")) {
                                JSONArray jsonArray = new JSONArray(serverResponse);
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("translations", jsonArray);
                                AppLogger.printJson(jsonObject);
                                listener.onSuccess(jsonObject);
                            } else {
                                JSONObject jsonObject = new JSONObject(serverResponse);
                                AppLogger.printJson(jsonObject);
                                listener.onSuccess(jsonObject);
                            }
                        } else {
                            listener.onSuccess("");
                        }
                    } else {
                        String serverResponse = response.errorBody().string();
                        JSONObject errorObject = new JSONObject(serverResponse);
                        listener.onError(errorObject);
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    listener.onError(e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onError(t);
            }
        });
    }

}

