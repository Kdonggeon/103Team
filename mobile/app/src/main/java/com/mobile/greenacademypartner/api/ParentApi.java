package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.attendance.Attendance;
import com.mobile.greenacademypartner.model.parent.AddChildrenRequest;
import com.mobile.greenacademypartner.model.parent.ParentSignupRequest;
import com.mobile.greenacademypartner.model.parent.ParentUpdateRequest;
import com.mobile.greenacademypartner.model.student.Student;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ParentApi {

    // ✅ 회원가입: Spring Boot의 @PostMapping("/api/parents")와 동일
    @POST("/api/parents")
    Call<Void> signupParent(@Body ParentSignupRequest request);

    // ✅ 학부모 정보 수정 (Authorization 필요)
    @PUT("/api/parents/{id}")
    Call<Void> updateParent(
            @Header("Authorization") String authHeader,
            @Path("id") String parentId,
            @Body ParentUpdateRequest request
    );

    // ✅ 학부모의 전체 출석 기록 (학부모 번호 기준)
    @GET("/api/parents/{parentsNumber}/attendance")
    Call<List<Attendance>> getAttendanceForParent(
            @Path("parentsNumber") String parentsNumber
    );

    // ✅ 자녀 개별 출석 기록
    @GET("/api/parents/{studentId}/attendance")
    Call<List<com.mobile.greenacademypartner.model.attendance.AttendanceResponse>>
    getAttendanceForChild(
            @Path("studentId") String childStudentId
    );

    // ✅ 학부모의 자녀 목록 조회
    @GET("/api/parents/{parentId}/children")
    Call<List<Student>> getChildrenByParentId(
            @Path("parentId") String parentId
    );

    // ✅ 자녀 이름 목록 조회
    @GET("/api/parents/{parentId}/children/names")
    Call<List<String>> getChildNames(
            @Path("parentId") String parentId
    );

    // ✅ 자녀 추가
    @POST("/api/parents/{parentId}/children")
    Call<Void> addChildren(
            @Header("Authorization") String auth,
            @Path("parentId") String parentId,
            @Body AddChildrenRequest request
    );

    // ✅ FCM 토큰 갱신
    @PUT("/api/parents/{id}/fcm-token")
    Call<Void> updateFcmToken(
            @Path("id") String parentId,
            @Header("Authorization") String authorization,
            @Body String fcmToken
    );

    // ✅ 학부모의 자녀 전체 출석 조회
    @GET("/api/parents/{parentId}/attendance")
    Call<List<Attendance>> getAllChildrenAttendance(
            @Path("parentId") String parentId
    );
}
