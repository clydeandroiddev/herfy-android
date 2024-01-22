package com.herfy.firebase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FirebaseApiConnector {

    private static FirebaseApiConnector instance;
    private Retrofit.Builder builder;
    private Retrofit retrofit;
    private HttpLoggingInterceptor logging;
    private okhttp3.OkHttpClient.Builder httpClient;

    private FirebaseApiConnector() {
        String baseUrl = "https://fcm.googleapis.com/";
        long connectionTimeout = 5L;
        long writeTimeout = 5L;
        long readTimeout = 5L;
        Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
        this.builder = (new Retrofit.Builder()).baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create(gson));
        this.retrofit = this.builder.build();
        this.logging = (new HttpLoggingInterceptor()).setLevel(HttpLoggingInterceptor.Level.BODY);
        this.httpClient = (new okhttp3.OkHttpClient.Builder()).connectTimeout(connectionTimeout, TimeUnit.MINUTES).writeTimeout(writeTimeout, TimeUnit.MINUTES).readTimeout(readTimeout, TimeUnit.MINUTES);
    }

    private FirebaseApiConnector(long connectionTimeout, long writeTimeout, long readTimeout) {
        String baseUrl = "https://fcm.googleapis.com/";
        Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
        this.builder = (new Retrofit.Builder()).baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create(gson));
        this.retrofit = this.builder.build();
        this.logging = (new HttpLoggingInterceptor()).setLevel(HttpLoggingInterceptor.Level.BODY);
        this.httpClient = (new okhttp3.OkHttpClient.Builder()).connectTimeout(connectionTimeout, TimeUnit.MINUTES).writeTimeout(writeTimeout, TimeUnit.MINUTES).readTimeout(readTimeout, TimeUnit.MINUTES);
    }

    public static FirebaseApiConnector getInstance() {
        return instance;
    }

    public static void init() {
        instance = new FirebaseApiConnector();
    }

    public static void init(long connectionTimeout, long writeTimeout, long readTimeout) {
        instance = new FirebaseApiConnector(connectionTimeout, writeTimeout, readTimeout);
    }

    public <S> S createService(Class<S> serviceClass) {
        if (!this.httpClient.interceptors().contains(this.logging)) {
            this.httpClient.addInterceptor(this.logging);
        }

        this.builder.client(this.httpClient.build());
        this.retrofit = this.builder.build();
        return this.retrofit.create(serviceClass);
    }
}

