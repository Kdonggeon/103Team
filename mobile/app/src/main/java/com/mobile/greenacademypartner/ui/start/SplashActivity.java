package com.mobile.greenacademypartner.ui.start;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.mobile.greenacademypartner.api.StudentApi;
//import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.ui.login.LoginActivity;
import com.mobile.greenacademypartner.ui.main.MainActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500; // 1.5초 지연

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔽 Android 13 이상은 알림 권한 직접 요청 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001);
            }
        }

        // FCM 토큰 발급 및 서버 전송
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        sendTokenToServer(token);
                    } else {
                        Log.w("SplashActivity", "FCM 토큰 가져오기 실패", task.getException());
                    }
                });

        // Splash 화면 지연 후 로그인 또는 메인으로 이동
        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
            boolean autoLogin = prefs.getBoolean("auto_login", false);
            Intent intent;
            if (isLoggedIn && autoLogin) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }

    // FCM 토큰을 서버에 전송하는 메서드
    private void sendTokenToServer(String token) {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String userId = prefs.getString("username", "");
        String role = prefs.getString("role", "student");

        if ("student".equalsIgnoreCase(role)) {
            StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
            api.updateFcmToken(userId, token)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            Log.d("SplashActivity", "FCM 토큰 전송 성공(학생)");
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.w("SplashActivity", "FCM 토큰 전송 실패(학생)", t);
                        }
                    });
//        } else {
//            TeacherApi api = RetrofitClient.getClient().create(TeacherApi.class);
//            api.updateFcmToken(userId, token)
//                    .enqueue(new Callback<Void>() {
//                        @Override
//                        public void onResponse(Call<Void> call, Response<Void> response) {
//                            Log.d("SplashActivity", "FCM 토큰 전송 성공(교사)");
//                        }
//
//                        @Override
//                        public void onFailure(Call<Void> call, Throwable t) {
//                            Log.w("SplashActivity", "FCM 토큰 전송 실패(교사)", t);
//                        }
//                    });
        }
    }
}
