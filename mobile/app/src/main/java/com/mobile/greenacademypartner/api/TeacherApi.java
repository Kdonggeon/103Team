package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.TeacherSignupRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface TeacherApi {
    @POST("/api/signup/teacher")
    Call<Void> signupTeacher(@Body TeacherSignupRequest request);
}
