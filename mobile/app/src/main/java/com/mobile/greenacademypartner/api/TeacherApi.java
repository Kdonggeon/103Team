//package com.mobile.greenacademypartner.api;
//
//
//import com.mobile.greenacademypartner.model.attendance.Attendance;
//import com.mobile.greenacademypartner.model.classes.Course;
//import com.mobile.greenacademypartner.model.classes.CreateClassRequest;
//import com.mobile.greenacademypartner.model.teacher.TeacherAttendance;
//import com.mobile.greenacademypartner.model.teacher.TeacherClass;
//import com.mobile.greenacademypartner.model.teacher.TeacherSignupRequest;
//import com.mobile.greenacademypartner.model.teacher.TeacherUpdateRequest;
//
//import java.util.List;
//
//import retrofit2.Call;
//import retrofit2.http.Body;
//import retrofit2.http.GET;
//import retrofit2.http.POST;
//import retrofit2.http.PUT;
//import retrofit2.http.Path;
//import retrofit2.http.Query;
//
//import com.mobile.greenacademypartner.model.teacher.TeacherSignupRequest;
//import retrofit2.Call;
//import retrofit2.http.Body;
//import retrofit2.http.POST;
//
//
//public interface TeacherApi {
//    @POST("/api/signup/teacher")
//    Call<Void> signupTeacher(@Body TeacherSignupRequest request);
//
//
//    @PUT("/api/teachers/{id}")
//    Call<Void> updateTeacher(@Path("id") String teacherId, @Body TeacherUpdateRequest request);
//
//    // 교사 ID로 수업 목록 조회
//    @GET("/api/teachers/{teacherId}/classes")
//    Call<List<TeacherClass>> getClassesForTeacher(@Path("teacherId") String teacherId);
//
//    // 특정 수업 ID로 출석 내역 조회
//    @GET("/api/teachers/classes/{classId}/attendance")
//    Call<List<TeacherAttendance>> getAttendanceForClass(
//            @Path("classId") String classId,
//            @Query("date") String date
//    );
//
//    @POST("/api/teachers/classes")
//    Call<Void> createClass(@Body CreateClassRequest request);
//
//    @GET("/api/teachers/{teacherId}/classes")
//    Call<List<Course>> getClassesByTeacherIdAndDate(
//            @Path("teacherId") String teacherId,
//            @Query("date") String date
//    );
//    @PUT("/api/teachers/{id}/fcm-token")
//    Call<Void> updateFcmToken(
//            @Path("id") String id,
//            @Query("token") String token
//    );
//
//}
