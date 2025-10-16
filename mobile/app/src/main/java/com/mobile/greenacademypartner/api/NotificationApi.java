package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.ui.notification.NotificationItem;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NotificationApi {

    // 목록 조회 (페이지네이션)
    // GET /api/notifications?userId=U123&page=1&pageSize=20
    @GET("/api/notifications")
    Call<List<NotificationItem>> getNotifications(
            @Query("userId") String userId,
            @Query("page") int page,
            @Query("pageSize") int pageSize
    );

    // 🔥 전체 삭제
    // DELETE /api/notifications?userId=U123
    @DELETE("/api/notifications")
    Call<Void> deleteAll(@Query("userId") String userId);

    // (옵션) 개별 삭제
    // DELETE /api/notifications/{id}?userId=U123
    @DELETE("/api/notifications/{id}")
    Call<Void> deleteOne(
            @Path("id") String notificationId,
            @Query("userId") String userId
    );
}
