package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.PasswordResetRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PasswordResetApi {
    @POST("/api/reset-password")
    Call<Void> resetPassword(@Body PasswordResetRequest request);
}

