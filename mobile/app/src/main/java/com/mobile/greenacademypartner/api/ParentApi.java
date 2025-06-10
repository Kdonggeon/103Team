package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.ParentSignupRequest;
import com.mobile.greenacademypartner.model.ParentUpdateRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ParentApi {
    @POST("/api/signup/parent")
    Call<Void> signupParent(@Body ParentSignupRequest request);

    @PUT("/api/parents/{id}")
    Call<Void> updateParent(@Path("id") String parentId, @Body ParentUpdateRequest request);
}
