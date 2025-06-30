package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.FindIdRequest;
import com.mobile.greenacademypartner.model.FindIdResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface FindIdApi {

    @Headers("Content-Type: application/json")
    @POST("/api/find-id")
    Call<FindIdResponse> findId(@Body FindIdRequest request);
}
