package com.mobile.greenacademypartner.ui.start;

import android.Manifest;
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
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
// import com.mobile.greenacademypartner.model.token.TokenRequest;  // ❌ 더 이상 안 씀
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

        // Android 13+ 알림 권한 요청 (POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }

        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences login = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String uid = login.getString("username", "");
        boolean enabled = settings.getBoolean("notifications_enabled_" + uid, true);

        if (enabled) {
            // 알림 ON → 현재 FCM 토큰 얻어서 서버 반영
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
            // 알림 OFF → 토큰 삭제 후 서버에 빈 값 업서트
            Log.d("SplashActivity", "알림 OFF → 토큰 삭제 및 서버 반영");
            FirebaseMessaging.getInstance().deleteToken()
                    .addOnCompleteListener(task -> sendTokenToServer(""));
        }

        // 스플래시 뒤 화면 전환
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

    /**
     * FCM 토큰 서버 전송
     * - role( student / parent )
     * - id 우선순위: 저장된 고유 id → username
     * - JWT(SharedPreferences.login_prefs.token)로 Authorization 헤더 생성해서 같이 전송
     */
    private void sendTokenToServer(String fcmToken) {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);

        String role = prefs.getString("role", "student"); // 기본 student
        String jwt = prefs.getString("token", null);
        if (jwt == null || jwt.trim().isEmpty()) {
            Log.w("Splash", "JWT 없음 → 서버에 FCM 토큰 전송 스킵");
            return;
        }
        String authHeader = "Bearer " + jwt.trim();

        String idStudent = prefs.getString("studentId", null);
        String idParent  = prefs.getString("parentId", null);
        String username  = firstNonEmpty(
                prefs.getString("userId", null),
                prefs.getString("username", null)
        );

        if ("student".equalsIgnoreCase(role)) {
            String id = firstNonEmpty(idStudent, username);
            if (id == null || id.trim().isEmpty()) {
                Log.w("Splash", "student 역할인데 studentId/username 없음 → 스킵");
                return;
            }

            StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
            api.updateFcmToken(
                    id,          // @Path("studentId")
                    authHeader,  // @Header("Authorization")
                    fcmToken     // ✅ 바디: 그냥 String
            ).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d("Splash", "학생 토큰 전송 성공");
                    } else {
                        Log.w("Splash", "학생 토큰 전송 실패 code=" + response.code());
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.w("Splash", "학생 토큰 전송 네트워크 실패", t);
                }
            });

        } else if ("parent".equalsIgnoreCase(role)) {
            String id = firstNonEmpty(idParent, username);
            if (id == null || id.trim().isEmpty()) {
                Log.w("Splash", "parent 역할인데 parentId/username 없음 → 스킵");
                return;
            }

            ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
            api.updateFcmToken(
                    id,          // @Path("id")
                    authHeader,  // @Header("Authorization")
                    fcmToken     // ✅ 바디: 그냥 String
            ).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d("Splash", "부모 토큰 전송 성공");
                    } else {
                        Log.w("Splash", "부모 토큰 전송 실패 code=" + response.code());
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.w("Splash", "부모 토큰 전송 네트워크 실패", t);
                }
            });
        } else {
            Log.d("Splash", "알 수 없는 role=" + role + " → 토큰 전송 안 함");
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
