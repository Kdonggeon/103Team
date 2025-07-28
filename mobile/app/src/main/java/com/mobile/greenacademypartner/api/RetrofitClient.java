package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.ui.qna.SimpleCookieJar;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "http://10.0.2.2:9090/";
    private static volatile Retrofit retrofit;

    private static final SimpleCookieJar cookieJar = new SimpleCookieJar();

    private RetrofitClient() {
        // private constructor
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .cookieJar(cookieJar)
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofit;
    }

}
