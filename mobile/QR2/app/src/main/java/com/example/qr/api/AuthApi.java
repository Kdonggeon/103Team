package com.example.qr.api;

import com.example.qr.model.login.LoginRequest;
import com.example.qr.model.login.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AuthApi {

    // ✅ 학생/학부모/교사/원장 모두 공용 로그인
    @Headers("Content-Type: application/json")
    @POST("/api/login") // ✅ 이 경로만 사용
    Call<LoginResponse> login(@Body LoginRequest request);

    // ✅ JWT 검증 (선택)
    @POST("/api/auth/verify")
    Call<Void> verify(@Header("Authorization") String bearerToken);
}
