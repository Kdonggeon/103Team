// src/main/java/com/example/qr/api/RetrofitClient.java
package com.example.qr.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "http://192.168.0.23:9090/";

    private static volatile Retrofit retrofit;
    private static volatile Context appContext;

    private RetrofitClient() {}

    /** Application.onCreate() 등에서 한 번 호출 */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    /** Retrofit 클라이언트 생성 */
    public static Retrofit getClient() {
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {

                    if (appContext == null) {
                        throw new IllegalStateException("⚠️ RetrofitClient.init(context)를 먼저 호출하세요.");
                    }

                    Gson gson = new GsonBuilder()
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .create();

                    // ✅ HTTP 로그 인터셉터
                    HttpLoggingInterceptor httpLog =
                            new HttpLoggingInterceptor(message -> Log.d("OKHTTP", message));
                    httpLog.setLevel(HttpLoggingInterceptor.Level.BODY);
                    httpLog.redactHeader("Authorization");
                    httpLog.redactHeader("Cookie");
                    httpLog.redactHeader("Set-Cookie");

                    // ✅ 토큰 자동 주입 인터셉터
                    Interceptor authInterceptor = chain -> {
                        Request original = chain.request();
                        String url = original.url().encodedPath();
                        Request.Builder builder = original.newBuilder();

                        SharedPreferences prefs = appContext.getSharedPreferences("academy_login", Context.MODE_PRIVATE);
                        String directorToken = prefs.getString("director_token", null);
                        String studentToken = prefs.getString("student_token", null);

                        // ✅ URL 별 토큰 구분 로직
                        if (url.contains("/api/attendance/check-in")) {
                            Log.d("OKHTTP", "⛔ check-in 요청: 자동 토큰 추가 안 함");
                        } else if (url.contains("/api/academy/") && studentToken != null) {
                            builder.header("Authorization", "Bearer " + studentToken);
                            Log.d("OKHTTP", "🧑‍🎓 학생 토큰 사용");
                        } else if (directorToken != null) {
                            builder.header("Authorization", "Bearer " + directorToken);
                            Log.d("OKHTTP", "👨‍🏫 원장 토큰 사용");
                        }

                        return chain.proceed(builder.build());
                    };

                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(authInterceptor)
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
