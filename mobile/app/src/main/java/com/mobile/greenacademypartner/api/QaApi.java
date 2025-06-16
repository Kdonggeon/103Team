package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.Qa;
import com.mobile.greenacademypartner.model.Qa.Answer;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface QaApi {

    @GET("/api/qna")
    Call<List<Qa>> getAllQa();

    @GET("/api/qna/{id}")
    Call<Qa> getQaById(@Path("id") String id);

    @POST("/api/qna")
    Call<Qa> createQa(@Body Qa qa);

    @PUT("/api/qna/{id}")
    Call<Qa> updateQa(@Path("id") String id, @Body Qa qa);

    @DELETE("/api/qna/{id}")
    Call<Void> deleteQa(@Path("id") String id);

    @POST("/api/qna/{id}/answers")
    Call<Answer> addAnswer(@Path("id") String qaId, @Body Answer answer);
}
