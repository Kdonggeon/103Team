package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.Answer;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface AnswerApi {

    // 답변 목록 조회
    @GET("/api/questions/{questionId}/answers")
    Call<List<Answer>> listAnswers(@Path("questionId") String questionId);

    //  답변 작성
    @POST("/api/questions/{questionId}/answers")
    Call<Answer> createAnswer(@Path("questionId") String questionId, @Body Answer answer);

    //  단일 답변 상세 조회
    @GET("/api/answers/{id}")
    Call<Answer> getAnswer(@Path("id") String id);

    //  답변 수정
    @PUT("/api/answers/{id}")
    Call<Answer> updateAnswer(@Path("id") String id, @Body Answer answer);

    @DELETE("/api/answers/{id}")
    Call<Void> deleteAnswer(@Path("id") String id);


}
