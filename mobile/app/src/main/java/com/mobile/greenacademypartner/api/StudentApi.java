package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.LoginRequest;
import com.mobile.greenacademypartner.model.LoginResponse;
import com.mobile.greenacademypartner.model.StudentSignupRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface StudentApi {

    @POST("/api/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/api/student/signup")
    Call<Void> signupStudent(@Body StudentSignupRequest request); // 이름 수정됨
}
