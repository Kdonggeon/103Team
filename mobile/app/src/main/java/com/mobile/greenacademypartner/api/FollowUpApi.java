package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.FollowUp;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface FollowUpApi {

    @GET("api/questions/{questionId}/followups")
    Call<List<FollowUp>> list(@Header("Authorization") String auth,
                              @Path("questionId") String questionId);

    @POST("api/questions/{questionId}/followups")
    Call<FollowUp> create(@Header("Authorization") String auth,
                          @Path("questionId") String questionId,
                          @Body FollowUp followUp);

    @PUT("api/followups/{id}")
    Call<FollowUp> update(@Header("Authorization") String auth,
                          @Path("id") String id,
                          @Body FollowUp followUp);

    @DELETE("api/followups/{id}")
    Call<Void> delete(@Header("Authorization") String auth,
                      @Path("id") String id);
}
