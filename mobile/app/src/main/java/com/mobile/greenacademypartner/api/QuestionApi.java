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
import retrofit2.http.Query;
import retrofit2.http.Header;

public interface QuestionApi {


    @GET("/api/questions/room")
    Call<Question> getOrCreateRoom(
            @Header("Authorization") String auth,
            @Query("academyNumber") int academyNumber
    );
    @PUT("/api/questions/{id}/read")
    Call<Void> markRead(
            @Header("Authorization") String auth,
            @Path("id") String questionId
    );

    @GET("/api/questions/{id}")
    Call<Question> getQuestion(@Path("id") String id);

    @POST("/api/questions")
    Call<Question> createQuestion(@Body Question question);

    @PUT("/api/questions/{id}")
    Call<Question> updateQuestion(@Path("id") String id, @Body Question question);

    @DELETE("/api/questions/{id}")
    Call<Void> deleteQuestion(@Path("id") String id);


    @GET("/api/questions/room")
    Call<Question> getOrCreateRoom(@Query("academyNumber") int academyNumber);

    @PUT("/api/questions/{id}/read")
    Call<Void> markRead(@Path("id") String questionId);
}
