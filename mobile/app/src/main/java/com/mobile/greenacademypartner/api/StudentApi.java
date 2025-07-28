package com.mobile.greenacademypartner.api;


import com.mobile.greenacademypartner.model.attendance.Attendance;

import com.mobile.greenacademypartner.model.login.LoginRequest;
import com.mobile.greenacademypartner.model.login.LoginResponse;
import com.mobile.greenacademypartner.model.student.StudentSignupRequest;
import com.mobile.greenacademypartner.model.student.Student;


import com.mobile.greenacademypartner.model.student.StudentUpdateRequest;


import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;




public interface StudentApi {

    @POST("/api/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @Headers("Content-Type: application/json")
    @POST("/api/students")
    Call<Student> signupStudent(@Body StudentSignupRequest request);

    @PUT("/api/students/{id}")
    Call<Void> updateStudent(@Path("id") String studentId, @Body StudentUpdateRequest request);

    @GET("/api/students/{studentId}/attendance")
    Call<List<Attendance>> getAttendanceRecords(@Path("studentId") String studentId);


    @GET("/api/students/{studentId}/attendance")
    Call<List<Attendance>> getAttendanceForStudent(@Path("studentId") String studentId);

    @GET("/api/students/{studentId}/attendance")
    Call<List<Attendance>> getAttendanceByStudentIdAndDate(
            @Path("studentId") String studentId,
            @Query("date") String date
    );
    @PUT("/api/students/{studentId}/fcm-token")
    Call<Void> updateFcmToken(
            @Path("studentId") String studentId,
            @Query("token") String token
    );




}
