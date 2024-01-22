package com.herfy.directions;

import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.BaseCall;

import okhttp3.ResponseBody;
import retrofit2.Call;

public class DirectionsApiCall extends BaseCall {

    private DirectionsHandler handler;

    public static DirectionsApiCall getInstance() {
        return new DirectionsApiCall();
    }

    private DirectionsApiCall() {
        handler = DirectionsApiConnector.getInstance().createService(DirectionsHandler.class);
    }

    public void getDirections(String key, String origin, String destination, ApiRequestListener apiRequestListener) {
        Call<ResponseBody> call = handler.getDirections(key, origin, destination);
        this.call(call, apiRequestListener);
    }

}
