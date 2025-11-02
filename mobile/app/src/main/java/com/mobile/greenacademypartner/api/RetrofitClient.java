package com.mobile.greenacademypartner.api;

import android.content.Context;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
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

    private static final String BASE_URL = "http://192.168.75.65:9090/";
    //코드상으로는 이거 127.0.0.1
    // 실제로 할때는 자신의 ip를 넣ㄹ어야함 192.168.75.65(진환 ip)
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

                    // GSON: snake_case ⇄ camelCase 자동 매핑
                    Gson gson = new GsonBuilder()
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .create();

                    // ★ 네트워크 상세 로그 (요청/응답 라인·헤더·본문)
                    HttpLoggingInterceptor httpLog =
                            new HttpLoggingInterceptor(message -> Log.d("OKHTTP", message));
                    httpLog.setLevel(HttpLoggingInterceptor.Level.BODY);

                    // 민감 헤더는 로그에서 마스킹
                    httpLog.redactHeader("Authorization");
                    httpLog.redactHeader("Cookie");
                    httpLog.redactHeader("Set-Cookie");

                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .cookieJar(cookieJar)
                            .addInterceptor(new AuthHeaderInterceptor(appContext)) // 기존 인증 헤더
                            .addInterceptor(httpLog)                                 // ★ 네트워크 로그
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
