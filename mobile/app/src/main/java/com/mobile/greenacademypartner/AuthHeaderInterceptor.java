package com.mobile.greenacademypartner;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthHeaderInterceptor implements Interceptor {
    private final Context appContext;

    public AuthHeaderInterceptor(Context appContext) {
        this.appContext = appContext.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        SharedPreferences p = appContext.getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        String token = p.getString("token", null);

        Request.Builder b = chain.request().newBuilder()
                .header("Accept", "application/json");

        if (token != null && !token.trim().isEmpty()) {
            b.header("Authorization", "Bearer " + token.trim());
        }
        return chain.proceed(b.build());
    }
}
