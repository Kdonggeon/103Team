package com.mobile.greenacademypartner.api;

import android.content.Context;

import com.mobile.greenacademypartner.ui.qna.SimpleCookieJar;
import com.mobile.greenacademypartner.net.BackendGuardInterceptor;
//import com.mobile.greenacademypartner.net.AuthHeaderInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "http://10.0.2.2:9090/";
    private static volatile Retrofit retrofit;

    private static final SimpleCookieJar cookieJar = new SimpleCookieJar();
    private static volatile Context appContext; // 인터셉터에 쓸 앱 컨텍스트

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

                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            // 쿠키 유지 (세션 기반이면 필수)
                            .cookieJar(cookieJar)

                            // 전역 가드: 오프라인/서버미기동/401/503 등에서 강제 로그아웃
                            .addInterceptor(new BackendGuardInterceptor(appContext))

//                            // 선택: 토큰 기반일 때만 사용 (쿠키만 쓰면 필요없으면 빼도 OK)
//                            .addInterceptor(new AuthHeaderInterceptor(appContext))

                            // 짧은 타임아웃 (개발 중 권장)
                            .callTimeout(3, TimeUnit.SECONDS)
                            .connectTimeout(2, TimeUnit.SECONDS)
                            .readTimeout(2, TimeUnit.SECONDS)
                            .writeTimeout(2, TimeUnit.SECONDS)
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofit;
    }
}
