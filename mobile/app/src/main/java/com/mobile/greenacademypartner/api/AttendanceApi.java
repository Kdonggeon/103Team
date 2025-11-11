package com.mobile.greenacademypartner.api;

import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AttendanceApi {

    @POST("/api/attendance/check-in")
    Call<ResponseBody> checkIn(
            @Header("Authorization") String authHeader,
            @Body Map<String, String> request
    );
}
