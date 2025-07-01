package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.Question;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface QuestionApi {

    @GET("/api/questions")
    Call<List<Question>> listQuestions();

    @GET("/api/questions/{id}")
    Call<Question> getQuestion(@Path("id") String id);

    @POST("/api/questions")
    Call<Question> createQuestion(@Body Question question);

    @PUT("/api/questions/{id}")
    Call<Question> updateQuestion(@Path("id") String id, @Body Question question);

    @DELETE("/api/questions/{id}")
    Call<Void> deleteQuestion(@Path("id") String id);
}
