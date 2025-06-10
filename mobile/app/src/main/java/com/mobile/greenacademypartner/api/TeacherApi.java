package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.TeacherSignupRequest;
import com.mobile.greenacademypartner.model.TeacherUpdateRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface TeacherApi {
    @POST("/api/signup/teacher")
    Call<Void> signupTeacher(@Body TeacherSignupRequest request);

    @PUT("/api/teachers/{id}")
    Call<Void> updateTeacher(@Path("id") String teacherId, @Body TeacherUpdateRequest request);

}
