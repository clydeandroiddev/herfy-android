package com.herfy.directions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DirectionsApiConnector {

    private static DirectionsApiConnector instance;
    private Retrofit.Builder builder;
    private Retrofit retrofit;
    private HttpLoggingInterceptor logging;
    private okhttp3.OkHttpClient.Builder httpClient;

    public static DirectionsApiConnector getInstance() {
        return instance;
    }


    public static void init(String url) {
        instance = new DirectionsApiConnector(url);
    }

    private DirectionsApiConnector(String url) {
        Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
        this.builder = (new Retrofit.Builder()).baseUrl(url).addConverterFactory(GsonConverterFactory.create(gson));
        this.retrofit = this.builder.build();
        this.logging = (new HttpLoggingInterceptor()).setLevel(HttpLoggingInterceptor.Level.BODY);
        this.httpClient = (new okhttp3.OkHttpClient.Builder()).connectTimeout(5L, TimeUnit.MINUTES).writeTimeout(5L, TimeUnit.MINUTES).readTimeout(5L, TimeUnit.MINUTES);
    }

    public <S> S createService(Class<S> serviceClass) {
        if (!this.httpClient.interceptors().contains(this.logging)) {
            this.httpClient.addInterceptor(this.logging);

            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            try {
                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                httpClient.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
                httpClient.hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }

            this.builder.client(this.httpClient.build());
            this.retrofit = this.builder.build();
        }

        return this.retrofit.create(serviceClass);
    }
}
