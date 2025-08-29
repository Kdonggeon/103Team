// DirectorApi.java
package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.director.DirectorSignupRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface DirectorApi {
    @POST("/api/directors/signup")
    Call<Void> signup(@Body DirectorSignupRequest req);
}
