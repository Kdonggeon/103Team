package com.mobile.greenacademypartner.net;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.mobile.greenacademypartner.util.SessionUtil;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthHeaderInterceptor implements Interceptor {

    private final Context appContext;

    public AuthHeaderInterceptor(Context appContext) {
        this.appContext = appContext.getApplicationContext();
    }

    @Nullable
    private String getToken() {
        SharedPreferences prefs = appContext.getSharedPreferences(SessionUtil.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("authToken", null);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String token = getToken();
        Request original = chain.request();
        if (token == null || token.isEmpty()) {
            return chain.proceed(original);
        }
        Request authed = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.proceed(authed);
    }
}
