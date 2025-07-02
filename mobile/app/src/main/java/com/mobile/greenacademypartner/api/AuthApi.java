package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.login.LoginRequest;
import com.mobile.greenacademypartner.model.login.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AuthApi {

    @Headers("Content-Type: application/json")
    @POST("/api/login")
    Call<LoginResponse> login(@Body LoginRequest request);
}
