package com.herfy.directions;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DirectionsHandler {

    @GET("json")
    Call<ResponseBody> getDirections(@Query("key") String key, @Query("origin") String origin, @Query("destination") String destination);

}
