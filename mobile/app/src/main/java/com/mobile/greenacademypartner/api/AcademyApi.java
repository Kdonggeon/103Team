package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.Academy;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface AcademyApi {
    @GET("/api/academy")
    Call<List<Academy>> getAcademyList();
}
