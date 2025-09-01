package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.FollowUp;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface FollowUpApi {

    @GET("/api/questions/{questionId}/followups")
    Call<List<FollowUp>> list(@Path("questionId") String questionId);

    @POST("/api/questions/{questionId}/followups")
    Call<FollowUp> create(@Path("questionId") String questionId, @Body FollowUp followUp);

    @PUT("/api/followups/{id}")
    Call<FollowUp> update(@Path("id") String id, @Body FollowUp followUp);

    @DELETE("/api/followups/{id}")
    Call<Void> delete(@Path("id") String id);
}
