package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.ParentSignupRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ParentApi {
    @POST("/api/signup/parent")
    Call<Void> signupParent(@Body ParentSignupRequest request);
}
