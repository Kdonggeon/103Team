package com.mobile.greenacademypartner.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RoomApi {

    // 입구 QR → waiting_room 에 기록
    @POST("/api/rooms/{roomNumber}/enter-lobby")
    Call<ResponseBody> enterLobby(
            @Path("roomNumber") int roomNumber,
            @Query("academyNumber") int academyNumber,
            @Query("studentId") String studentId
    );

    // 좌석 QR → check-in
    @PUT("/api/rooms/{roomNumber}/check-in")
    Call<ResponseBody> checkIn(
            @Path("roomNumber") int roomNumber,
            @Query("academyNumber") int academyNumber,
            @Query("seatNumber") int seatNumber,   // 백엔드가 seatNumber/seat 둘 다 받게 해둠
            @Query("studentId") String studentId
    );
}