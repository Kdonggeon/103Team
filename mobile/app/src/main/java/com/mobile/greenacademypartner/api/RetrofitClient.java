package com.mobile.greenacademypartner.api;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mobile.greenacademypartner.AuthHeaderInterceptor;
import com.mobile.greenacademypartner.ui.qna.SimpleCookieJar;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    public static final String BASE_URL = "http://localhost:9090/";

    private static volatile Retrofit retrofit;

    private static final SimpleCookieJar cookieJar = new SimpleCookieJar();
    private static volatile Context appContext;

    /** Application.onCreate()에서 한 번 호출 */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    private RetrofitClient() {}

    public static Retrofit getClient() {
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {

                    if (appContext == null) {
                        throw new IllegalStateException(
                                "RetrofitClient.init(context)를 먼저 호출하세요.");
                    }

                    // ✅ GSON: 기본 설정 (카멜케이스 유지)
                    Gson gson = new GsonBuilder()
                            .create();

                    // ★ 네트워크 상세 로그
                    HttpLoggingInterceptor httpLog =
                            new HttpLoggingInterceptor(message -> Log.d("OKHTTP", message));
                    httpLog.setLevel(HttpLoggingInterceptor.Level.BODY);

                    httpLog.redactHeader("Authorization");
                    httpLog.redactHeader("Cookie");
                    httpLog.redactHeader("Set-Cookie");

                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .cookieJar(cookieJar)
                            .addInterceptor(new AuthHeaderInterceptor(appContext))
                            .addInterceptor(httpLog)
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(20, TimeUnit.SECONDS)
                            .writeTimeout(20, TimeUnit.SECONDS)
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();
                }
            }
        }
        return retrofit;
    }
}
