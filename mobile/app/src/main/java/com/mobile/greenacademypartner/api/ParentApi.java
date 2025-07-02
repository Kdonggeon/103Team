package com.mobile.greenacademypartner.api;


import com.mobile.greenacademypartner.model.attendance.Attendance;
import com.mobile.greenacademypartner.model.parent.ParentSignupRequest;
import com.mobile.greenacademypartner.model.parent.ParentUpdateRequest;
import com.mobile.greenacademypartner.model.student.Student;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;


public interface ParentApi {
    @POST("/api/signup/parent")
    Call<Void> signupParent(@Body ParentSignupRequest request);


    @PUT("/api/parents/{id}")
    Call<Void> updateParent(@Path("id") String parentId, @Body ParentUpdateRequest request);

    @GET("/api/parents/{parentsNumber}/attendance")
    Call<List<Attendance>> getAttendanceForParent(@Path("parentsNumber") String parentsNumber);

    @GET("/api/parents/{studentId}/attendance")
    Call<List<Attendance>> getAttendanceForChild(@Path("studentId") String studentId);

    @GET("/api/parents/{parentId}/children")
    Call<List<Student>> getChildrenByParentId(@Path("parentId") String parentId);





}
