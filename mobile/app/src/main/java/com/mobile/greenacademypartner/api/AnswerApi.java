package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.Answer;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface AnswerApi {

    @GET("api/questions/{questionId}/answers")
    Call<List<Answer>> listAnswers(@Header("Authorization") String auth,
                                   @Path("questionId") String questionId);

    @POST("api/questions/{questionId}/answers")
    Call<Answer> createAnswer(@Header("Authorization") String auth,
                              @Path("questionId") String questionId,
                              @Body Answer answer);

    @GET("api/answers/{id}")
    Call<Answer> getAnswer(@Header("Authorization") String auth,
                           @Path("id") String id);

    @PUT("api/answers/{id}")
    Call<Answer> updateAnswer(@Header("Authorization") String auth,
                              @Path("id") String id,
                              @Body Answer answer);

    @DELETE("api/answers/{id}")
    Call<Void> deleteAnswer(@Header("Authorization") String auth,
                            @Path("id") String id);
}
