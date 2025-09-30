package com.mobile.greenacademypartner.ui.start;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;

import com.mobile.greenacademypartner.ui.login.LoginActivity;
import com.mobile.greenacademypartner.ui.main.MainActivity;

import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500; // 1.5초 지연

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Android 13+ 알림 권한
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001);
            }
        }

        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences login = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String uid = login.getString("username", "");
        boolean enabled = settings.getBoolean("notifications_enabled_" + uid, true);

        if (enabled) {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String token = task.getResult();
                            sendTokenToServer(token);
                        } else {
                            Log.w("SplashActivity", "FCM 토큰 가져오기 실패", task.getException());
                        }
                    });
        } else {
            Log.d("SplashActivity", "알림 OFF → 토큰 삭제 및 서버 반영");
            FirebaseMessaging.getInstance().deleteToken()
                    .addOnCompleteListener(task -> sendTokenToServer(""));
        }

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
            boolean autoLogin = prefs.getBoolean("auto_login", false);
            Intent intent = (isLoggedIn && autoLogin)
                    ? new Intent(this, MainActivity.class)
                    : new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }

    // FCM 토큰 서버에 전송(ID 폴백 포함)
    private void sendTokenToServer(String token) {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", "student");

        String idStudent = prefs.getString("studentId", null);
        String idTeacher = prefs.getString("teacherId", null);
        String idParent = prefs.getString("parentId", null);
        String username = firstNonEmpty(prefs.getString("userId", null),
                prefs.getString("username", null));

        if ("student".equalsIgnoreCase(role)) {
            String id = firstNonEmpty(idStudent, username);
            if (id == null) return;
            StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
            api.updateFcmToken(id, token).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    Log.d("Splash", "학생 토큰 전송 성공");
                }


                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.w("Splash", "학생 토큰 전송 실패", t);
                }
            });

        } else if ("parent".equalsIgnoreCase(role)) {
            String id = firstNonEmpty(idParent, username);
            if (id == null) return;
            ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
            api.updateFcmToken(id, token).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    Log.d("Splash", "부모 토큰 전송 성공");
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.w("Splash", "부모 토큰 전송 실패", t);
                }
            });

        }
    }


    private String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) return v;
        }
        return null;
    }
}

