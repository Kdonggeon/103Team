package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.Attendance;
import com.mobile.greenacademypartner.model.Course;
import com.mobile.greenacademypartner.model.TeacherSignupRequest;
import com.mobile.greenacademypartner.model.TeacherUpdateRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface TeacherApi {

    @POST("/api/signup/teacher")
    Call<Void> signupTeacher(@Body TeacherSignupRequest request);

    @PUT("/api/teachers/{id}")
    Call<Void> updateTeacher(@Path("id") String teacherId, @Body TeacherUpdateRequest request);

    // ✅ 교사 ID로 수업 리스트 조회
    @GET("/api/teachers/{teacherId}/classes")
    Call<List<Course>> getCoursesByTeacherId(@Path("teacherId") String teacherId);

    // ✅ 수업 ID로 출석 내역 조회
    @GET("/api/classes/{classId}/attendance")
    Call<List<Attendance>> getAttendanceByClassId(@Path("classId") String classId);

}
