package com.mobile.greenacademypartner.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RoomApi {
    @PUT("/api/rooms/{roomNumber}/check-in")
    Call<ResponseBody> checkIn(
            @Path("roomNumber") int roomNumber,
            @Query("academyNumber") int academyNumber,
            @Query("seatNumber") int seatNumber,
            @Query("studentId") String studentId
    );
}