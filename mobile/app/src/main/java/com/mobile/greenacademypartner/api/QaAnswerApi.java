package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.QaAnswer;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface QaAnswerApi {

    // ✅ 사용 가능한 실제 POST API
    @POST("/api/answers")
    Call<QaAnswer> createAnswer(@Body QaAnswer answer);

    // ✅ 사용 가능한 GET API
    @GET("/api/answers/qa/{qaId}")
    Call<QaAnswer> getAnswerByQaId(@Path("qaId") String qaId);
}
