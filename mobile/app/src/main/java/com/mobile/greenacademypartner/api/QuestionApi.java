package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.Question;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface QuestionApi {

    @GET("api/questions/{id}")
    Call<Question> getQuestion(@Header("Authorization") String auth,
                               @Path("id") String id);

    @POST("api/questions")
    Call<Question> createQuestion(@Header("Authorization") String auth,
                                  @Body Question question);

    @PUT("api/questions/{id}")
    Call<Question> updateQuestion(@Header("Authorization") String auth,
                                  @Path("id") String id,
                                  @Body Question question);

    @DELETE("api/questions/{id}")
    Call<Void> deleteQuestion(@Header("Authorization") String auth,
                              @Path("id") String id);

    @PUT("api/questions/{id}/read")
    Call<Void> markRead(@Header("Authorization") String auth,
                        @Path("id") String questionId);

    @GET("api/questions/room/parent")
    Call<Question> getOrCreateParentRoom(@Header("Authorization") String auth,
                                         @Query("academyNumber") int academyNumber);

    @GET("api/questions/room")
    Call<Question> getOrCreateRoom(@Header("Authorization") String auth,
                                   @Query("academyNumber") int academyNumber,
                                   @Query("studentId") String studentId);
    @GET("api/questions/room/parent/for-teacher")
    Call<Question> getOrCreateParentRoomForTeacher(@Header("Authorization") String auth,
                                                   @Query("academyNumber") int academyNumber,
                                                   @Query("parentId") String parentId);

    @GET("api/questions/room/by-id")
    Call<Question> getOrCreateRoomById(@Header("Authorization") String auth,
                                       @Query("academyNumber") int academyNumber,
                                       @Query("id") String id);


    @POST("api/questions/room/parent")
    Call<Question> createParentRoom(@Header("Authorization") String auth,
                                    @Query("academyNumber") int academyNumber);
}
