package com.mobile.greenacademypartner.api;

import com.mobile.greenacademypartner.model.Academy;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface AcademyApi {

    // 🔥 전체 학원 목록 조회
    @GET("/api/academy")
    Call<List<Academy>> getAcademyList();

    // 🔥 단일 학원 조회 (학원 번호로)
    @GET("/api/academy/{academyNumber}")
    Call<Academy> getAcademy(@Path("academyNumber") int academyNumber);
}
