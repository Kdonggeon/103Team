package com.example.qr.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

import java.util.Map;

public interface AttendanceApi {

    @POST("/api/attendance/check-in")
    Call<ResponseBody> checkIn(
            @Header("Authorization") String authorization,
            @Body Map<String, String> req
    );
}
