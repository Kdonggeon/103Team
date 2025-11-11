package com.example.qr.api;

import com.example.qr.model.Student.Student;
import com.example.qr.model.academy.ClassInfo; // ✅ 새로 추가 (수업 정보용 모델)
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface AcademyApi {

    // ✅ 특정 학원의 학생 목록 조회
    @GET("/api/academy/{academyNumber}/students")
    Call<List<Student>> getStudentsByAcademy(
            @Header("Authorization") String authHeader,
            @Path("academyNumber") String academyNumber
    );

    // ✅ 특정 학원의 수업 목록 조회 (classId 자동 선택용)
    @GET("/api/academy/{academyNumber}/classes")
    Call<List<ClassInfo>> getClassesByAcademy(
            @Header("Authorization") String authHeader,
            @Path("academyNumber") String academyNumber
    );
}
