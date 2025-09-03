package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.login.LoginRequest;
import com.mobile.greenacademypartner.model.login.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AuthApi {

    // 로그인
    @Headers("Content-Type: application/json")
    @POST("/api/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // 추가: 토큰 검증 (백엔드에 맞춰 경로 수정)
    @POST("/api/auth/verify")
    Call<Void> verify(@Header("Authorization") String bearerToken);
}
