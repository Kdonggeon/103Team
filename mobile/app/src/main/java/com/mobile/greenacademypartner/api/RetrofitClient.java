package com.mobile.greenacademypartner.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:9090/")
                    // 또는 실제 서버 IP
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

        }
        return retrofit;


    }
}