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

    @POST("/api/signup/parent")
    Call<Void> signupParent(@Body ParentSignupRequest request);

    // ✅ Authorization 헤더 추가 (DB 수정 요청에는 항상 필요)
    @PUT("/api/parents/{id}")
    Call<Void> updateParent(
            @Header("Authorization") String authHeader,
            @Path("id") String parentId,
            @Body ParentUpdateRequest request
    );

    @GET("/api/parents/{parentsNumber}/attendance")
    Call<List<Attendance>> getAttendanceForParent(
            @Path("parentsNumber") String parentsNumber
    );

    @GET("/api/parents/{studentId}/attendance")
    Call<List<com.mobile.greenacademypartner.model.attendance.AttendanceResponse>>
    getAttendanceForChild(
            @Path("studentId") String childStudentId
    );

    @GET("/api/parents/{parentId}/children")
    Call<List<Student>> getChildrenByParentId(
            @Path("parentId") String parentId
    );

    @GET("/api/parents/{parentId}/children/names")
    Call<List<String>> getChildNames(
            @Path("parentId") String parentId
    );

    @POST("/api/parents/{parentId}/children")
    Call<Void> addChildren(
            @Header("Authorization") String auth,
            @Path("parentId") String parentId,
            @Body AddChildrenRequest request
    );

    @PUT("/api/parents/{id}/fcm-token")
    Call<Void> updateFcmToken(
            @Path("id") String parentId,
            @Header("Authorization") String authorization,
            @Body String fcmToken
    );

    @GET("/api/parents/{parentId}/attendance")
    Call<List<Attendance>> getAllChildrenAttendance(
            @Path("parentId") String parentId
    );
}
