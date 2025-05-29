package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.Student;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface StudentApi {
    @GET("/api/students")
    Call<List<Student>> getAllStudents();
}
