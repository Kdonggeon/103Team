package com.mobile.greenacademypartner.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.messaging.FirebaseMessaging;
import com.mobile.greenacademypartner.ui.login.LoginActivity;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.ui.timetable.ParentChildrenListActivity;
import com.mobile.greenacademypartner.ui.timetable.StudentTimetableActivity;
import com.mobile.greenacademypartner.ui.timetable.TeacherTimetableActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private static final int REQ_POST_NOTI = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);

        // Android 13 이상 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTI
                );
            }
        }

        // FCM 토큰 획득 및 서버 전송 (앱 실행 시마다 호출)
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        sendTokenToServer(token);
                    } else {
                        Log.w("MainActivity", "FCM 토큰 가져오기 실패", task.getException());
                    }
                });

        // 로그인 여부 확인 및 분기 처리
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        if (!isLoggedIn) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // 로그인되어 있음 → 역할별 메인 화면으로 분기
        String role = prefs.getString("role", "student");
        Log.d("MainActivity", "로그인된 사용자 role: " + role);

        Intent intent;
        switch (role.toLowerCase()) {
            case "student":
                intent = new Intent(this, StudentTimetableActivity.class);
                break;
            case "teacher":
                intent = new Intent(this, TeacherTimetableActivity.class);
                break;
            case "parent":
                intent = new Intent(this, ParentChildrenListActivity.class);
                break;
            default:
                intent = new Intent(this, LoginActivity.class);
                break;
        }

        startActivity(intent);
        finish();
    }

    private void sendTokenToServer(String token) {
        String userId = prefs.getString("username", "");
        String role = prefs.getString("role", "student");

        if ("student".equalsIgnoreCase(role)) {
            StudentApi api = RetrofitClient.getClient()
                    .create(StudentApi.class);
            api.updateFcmToken(userId, token)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> res) {
                            Log.d("MainActivity", "FCM 토큰 전송 성공(학생)");
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.w("MainActivity", "FCM 토큰 전송 실패(학생)", t);
                        }
                    });
        } else {
            TeacherApi api = RetrofitClient.getClient()
                    .create(TeacherApi.class);
            api.updateFcmToken(userId, token)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> res) {
                            Log.d("MainActivity", "FCM 토큰 전송 성공(교사)");
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.w("MainActivity", "FCM 토큰 전송 실패(교사)", t);
                        }
                    });
        }

    }
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_POST_NOTI) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 허용됨
            } else {
                // 권한 거부됨 → 알림이 표시되지 않을 수 있음
            }
        }
    }
}

