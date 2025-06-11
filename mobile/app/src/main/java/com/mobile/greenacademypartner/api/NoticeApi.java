package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.Notice;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface NoticeApi {
    @GET("/api/notices")
    Call<List<Notice>> getNotices();

    @GET("/api/notices/{id}")
    Call<Notice> getNotice(@Path("id") String id);

    @POST("/api/notices")
    Call<Notice> createNotice(@Body Notice notice);
}
