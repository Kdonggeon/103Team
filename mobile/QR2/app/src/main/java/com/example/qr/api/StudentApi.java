package com.example.qr.api;

import com.example.qr.model.Student.Student;
import com.example.qr.model.attendance.Attendance;
import com.example.qr.model.attendance.AttendanceResponse;
import com.example.qr.model.login.LoginRequest;
import com.example.qr.model.login.LoginResponse;
import com.example.qr.model.Student.StudentSignupRequest;
import com.example.qr.model.Student.StudentUpdateRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
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
    Call<Void> updateStudent(
            @Header("Authorization") String authHeader,
            @Path("id") String studentId,
            @Body StudentUpdateRequest request
    );

    @GET("/api/students/{studentId}/attendance")
    Call<List<Attendance>> getAttendanceRecords(
            @Path("studentId") String studentId
    );

    @GET("/api/students/{studentId}/attendance")
    Call<List<AttendanceResponse>> getAttendanceForStudent(
            @Path("studentId") String studentId
    );

    @GET("/api/students/{studentId}/attendance")
    Call<List<AttendanceResponse>> getAttendanceForStudentByAcademy(
            @Path("studentId") String studentId,
            @Query("academyNumber") int academyNumber
    );

    @GET("/api/students/{studentId}/attendance")
    Call<List<Attendance>> getAttendanceByStudentIdAndDate(
            @Path("studentId") String studentId,
            @Query("date") String date
    );

    @PUT("/api/students/{studentId}/fcm-token")
    Call<Void> updateFcmToken(
            @Path("studentId") String studentId,
            @Header("Authorization") String authorization,
            @Body String fcmToken
    );

    @GET("/api/students/{studentId}")
    Call<Student> getStudentById(
            @Path("studentId") String studentId
    );

    @POST("/api/students/{id}/attendance")
    Call<Void> checkAttendance(
            @Header("Authorization") String authHeader,
            @Path("id") String studentId
    );
}
