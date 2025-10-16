package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.attendance.Attendance;
import com.mobile.greenacademypartner.model.attendance.AttendanceResponse;
import com.mobile.greenacademypartner.model.classes.Course;
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

    // ✅ 학생 전체 출결
    @GET("/api/students/{studentId}/attendance")
    Call<List<Attendance>> getAttendanceRecords(@Path("studentId") String studentId);

    // ✅ 응답 확장형 (AttendanceResponse)
    @GET("/api/students/{studentId}/attendance")
    Call<List<AttendanceResponse>> getAttendanceForStudent(@Path("studentId") String studentId);

    // ✅ 🔥 학원 번호별 필터링 추가
    @GET("/api/students/{studentId}/attendance")
    Call<List<AttendanceResponse>> getAttendanceForStudentByAcademy(
            @Path("studentId") String studentId,
            @Query("academyNumber") int academyNumber
    );

    // 특정 날짜 기준 출결
    @GET("/api/students/{studentId}/attendance")
    Call<List<Attendance>> getAttendanceByStudentIdAndDate(
            @Path("studentId") String studentId,
            @Query("date") String date
    );

    // FCM 토큰 업데이트
    @PUT("/api/students/{studentId}/fcm-token")
    Call<Void> updateFcmToken(
            @Path("studentId") String studentId,
            @Query("token") String token
    );

    // 학생의 수업 목록
    @GET("/api/students/{studentId}/classes")
    Call<List<Course>> getMyClasses(@Path("studentId") String studentId);

    // 부모 → 자녀 목록
    @GET("/api/parents/{parentId}/students")
    Call<List<Student>> getStudentsByParentId(@Path("parentId") String parentId);

    // 학생 단일 조회
    @GET("/api/students/{studentId}")
    Call<Student> getStudentById(@Path("studentId") String studentId);
}
