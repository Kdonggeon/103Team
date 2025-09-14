package com.mobile.greenacademypartner.api;

import android.content.Context;

import com.mobile.greenacademypartner.net.BackendGuardInterceptor;
import com.mobile.greenacademypartner.ui.qna.SimpleCookieJar;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "http://10.0.2.2:9090/";
    private static volatile Retrofit retrofit;

    private static final SimpleCookieJar cookieJar = new SimpleCookieJar();
    private static volatile Context appContext; // 인터셉터에서 사용

    private RetrofitClient() {}

    /** Application.onCreate()에서 꼭 한 번 호출 */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {
                    if (appContext == null) {
                        throw new IllegalStateException("RetrofitClient not initialized. Call RetrofitClient.init(context) first.");
                    }

                    HttpLoggingInterceptor log = new HttpLoggingInterceptor();
                    log.setLevel(HttpLoggingInterceptor.Level.BASIC);

                    OkHttpClient ok = new OkHttpClient.Builder()
                            .cookieJar(cookieJar)
                            .addInterceptor(new BackendGuardInterceptor(appContext))
                            // .addInterceptor(new AuthHeaderInterceptor(appContext)) // 토큰 사용할 때만
                            .addInterceptor(log) // 개발 중 로그
                            .callTimeout(3, TimeUnit.SECONDS)
                            .connectTimeout(2, TimeUnit.SECONDS)
                            .readTimeout(2, TimeUnit.SECONDS)
                            .writeTimeout(2, TimeUnit.SECONDS)
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(ok)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofit;
    }
}
