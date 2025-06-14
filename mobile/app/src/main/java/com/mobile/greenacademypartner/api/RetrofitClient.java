package com.mobile.greenacademypartner.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mobile.greenacademypartner.model.LoginResponse;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // 👉 커스텀 Deserializer 등록
            Gson gson = new GsonBuilder().create();


            retrofit = new Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:9090/")
                    .addConverterFactory(GsonConverterFactory.create()) // 기본 GSON 사용
                    .build();

        }
        return retrofit;
    }
}
