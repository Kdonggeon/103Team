package com.mobile.greenacademypartner.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.messaging.FirebaseMessaging;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.ui.login.LoginActivity;
import com.mobile.greenacademypartner.ui.timetable.ParentChildrenListActivity;
import com.mobile.greenacademypartner.ui.timetable.StudentTimetableActivity;
import com.mobile.greenacademypartner.ui.timetable.TeacherTimetableActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// SessionUtil: safe, PREFS_NAME, isNetworkAvailable, clearLoginAndGoLogin
import static com.mobile.greenacademypartner.util.SessionUtil.*;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQ_POST_NOTI = 1001;

    // 재시도(FCM 전송 전용)
    private static final int MAX_RETRY = 1;
    private static final long RETRY_DELAY_MS = 1500L;
    private int retryCount = 0;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 0) 알림 권한(안드13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTI);
            }
        }

        // 1) 자동 로그인 최소 요건 점검 (4요소)
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        String username = safe(prefs.getString("username", ""));
        String role = safe(prefs.getString("role", "")).toLowerCase();
        String token = safe(prefs.getString("token", ""));
        if (token.isEmpty()) token = safe(prefs.getString("accessToken", ""));

        if (!isLoggedIn || username.isEmpty() || role.isEmpty() || token.isEmpty()) {
            clearLoginAndGoLogin(this, "Missing fields");
            return;
        }

        // 2) 네트워크 체크 (권한: ACCESS_NETWORK_STATE 필요)
        if (!isNetworkAvailable(this)) {
            clearLoginAndGoLogin(this, "No network");
            return;
        }

        // 3) 헬스체크 없이 바로 라우팅 & FCM 등록
        routeByRole(role);
        fetchAndSendFcmToken(username, role);
    }

    // ───────── 역할 라우팅 ─────────
    private void routeByRole(String role) {
        Intent intent;
        switch (role) {
            case "student":
                intent = new Intent(this, StudentTimetableActivity.class);
                break;
            case "teacher":
            case "director": // 원장도 교사용 시간표로
                intent = new Intent(this, TeacherTimetableActivity.class);
                break;
            case "parent":
                intent = new Intent(this, ParentChildrenListActivity.class);
                break;
            default:
                clearLoginAndGoLogin(this, "Unknown role");
                return;
        }
        startActivity(intent);
        finish();
    }

    private void goLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // ───────── FCM 토큰 획득 & 서버 전송 ─────────
    private void fetchAndSendFcmToken(String username, String role) {
        if (username.isEmpty() || role.isEmpty()) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "FCM 토큰 가져오기 실패", task.getException());
                        return;
                    }
                    String fcmToken = task.getResult();
                    sendTokenToServer(username, role, fcmToken);
                });
    }

    private void sendTokenToServer(String userId, String role, String fcmToken) {
        if (fcmToken == null || fcmToken.isEmpty()) return;

        if ("student".equalsIgnoreCase(role)) {
            StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
            api.updateFcmToken(userId, fcmToken).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> res) {
                    Log.d(TAG, "FCM 토큰 전송 성공(학생)");
                }
                @Override public void onFailure(Call<Void> call, Throwable t) {
                    Log.w(TAG, "FCM 토큰 전송 실패(학생): " + t.getMessage());
                    maybeRetryFcm(userId, role, fcmToken);
                }
            });
        } else { // teacher / director / parent (교사 API로 처리하는 기존 흐름 유지)
            TeacherApi api = RetrofitClient.getClient().create(TeacherApi.class);
            api.updateFcmToken(userId, fcmToken).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> res) {
                    Log.d(TAG, "FCM 토큰 전송 성공(교사/원장/부모)");
                }
                @Override public void onFailure(Call<Void> call, Throwable t) {
                    Log.w(TAG, "FCM 토큰 전송 실패(교사/원장/부모): " + t.getMessage());
                    maybeRetryFcm(userId, role, fcmToken);
                }
            });
        }
    }

    // FCM 전송 1회 재시도
    private void maybeRetryFcm(String userId, String role, String fcmToken) {
        if (retryCount < MAX_RETRY) {
            retryCount++;
            new Handler(getMainLooper()).postDelayed(
                    () -> sendTokenToServer(userId, role, fcmToken),
                    RETRY_DELAY_MS
            );
        }
    }

    // ───────── Util (디버깅용) ─────────
    @SuppressWarnings("unused")
    private void debugDumpPrefs() {
        SharedPreferences p = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Log.d(TAG,
                "is_logged_in=" + p.getBoolean("is_logged_in", false) + "\n" +
                        "auto_login=" + p.getBoolean("auto_login", false) + "\n" +
                        "username=" + p.getString("username", "") + "\n" +
                        "role=" + p.getString("role", "") + "\n" +
                        "token.len=" + safe(p.getString("token", "")).length() + "\n" +
                        "accessToken.len=" + safe(p.getString("accessToken", "")).length()
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_POST_NOTI) {
            // 허용/거부에 따라 안내만 (알림 미표시 가능)
        }
    }
}
