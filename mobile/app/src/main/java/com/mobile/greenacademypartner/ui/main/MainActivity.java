package com.mobile.greenacademypartner.ui.main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

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

// ← 추가
import com.mobile.greenacademypartner.api.AuthApi;

import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "login_prefs";
    private static final int REQ_POST_NOTI = 1001;

    // 재시도(FCM/검증 공용)
    private static final int MAX_RETRY = 1;
    private static final long RETRY_DELAY_MS = 1500L;
    private int retryCount = 0;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 알림 권한(안드13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTI);
            }
        }

        // 자동 로그인 최소 요건 점검
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        String username = safe(prefs.getString("username", ""));
        String role = safe(prefs.getString("role", "")).toLowerCase();
        String token = safe(prefs.getString("token", ""));
        if (token.isEmpty()) token = safe(prefs.getString("accessToken", ""));

        if (!isLoggedIn || username.isEmpty() || role.isEmpty() || token.isEmpty()) {
            Log.d(TAG, "Auto login failed (missing fields) → goLogin()");
            goLogin();
            return;
        }

        // ★ 오프라인/백엔드 불가 시 강제로그아웃 플로우
        verifyBackendAndRoute(username, role, token);
    }

    // ───────── 오프라인/백엔드 확인 후 라우팅 ─────────
    // ★ 토큰 검증 API 없이: 오프라인/서버 미기동이면 강제 로그아웃
    private void verifyBackendAndRoute(String username, String role, String token) {
        // 1) 오프라인 → 즉시 로그아웃
        if (!isOnline()) {
            Log.w(TAG, "Device offline → forceLogout()");
            toast("인터넷 연결이 필요합니다. 다시 로그인해 주세요.");
            forceLogout();
            return;
        }

        // 2) 백엔드 도달 가능 여부만 확인 (HEAD / or 404도 '접속 성공'으로 간주)
        pingBackend(new Runnable() {
            @Override public void run() {
                // 접속 성공 → 라우팅 + FCM 등록
                routeByRole(role);
                fetchAndSendFcmToken(username, role);
            }
        }, new Runnable() {
            @Override public void run() {
                // 접속 실패(서버 꺼짐/미기동/네트워크 오류) → 강제 로그아웃
                Log.w(TAG, "Backend unreachable → forceLogout()");
                toast("서버에 연결할 수 없습니다. 다시 로그인해 주세요.");
                forceLogout();
            }
        });
    }

    /**
     * 백엔드 “접속 가능”만 비동기로 체크한다.
     * - Retrofit의 baseUrl()로 HEAD 요청을 보낸다.
     * - HTTP 코드가 200~599이면 ‘접속 성공’(서버가 살아있음)으로 본다.
     * - 네트워크 예외(IOException 등) 시 ‘접속 실패’.
     */
    private void pingBackend(Runnable onReachable, Runnable onUnreachable) {
        try {
            // Retrofit 인스턴스에서 baseUrl을 얻는다.
            retrofit2.Retrofit retrofit = com.mobile.greenacademypartner.api.RetrofitClient.getClient();
            okhttp3.HttpUrl url = retrofit.baseUrl();

            // HEAD / (루트로 HEAD). 서버가 라우팅 없으면 404여도 ‘접속은 됨’으로 간주.
            okhttp3.Request req = new okhttp3.Request.Builder()
                    .url(url)
                    .head()
                    .build();

            // 타임아웃 짧게
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .callTimeout(3, TimeUnit.SECONDS)
                    .connectTimeout(2, TimeUnit.SECONDS)
                    .readTimeout(2, TimeUnit.SECONDS)
                    .writeTimeout(2, TimeUnit.SECONDS)
                    .build();

            client.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    runOnUiThread(onUnreachable);
                }

                @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                    response.close();
                    // 어떤 HTTP 코드든 응답 받았으면 서버는 ‘켜져 있음’으로 판단
                    runOnUiThread(onReachable);
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "pingBackend exception: " + e.getMessage());
            runOnUiThread(onUnreachable);
        }
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
                Log.w(TAG, "Unknown role: " + role + " → goLogin()");
                goLogin();
                return;
        }
        startActivity(intent);
        finish();
    }

    private void goLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void forceLogout() {
        // 로그인 관련 키만 정리 (필요시 prefs.edit().clear()로 전체삭제)
        prefs.edit()
                .putBoolean("is_logged_in", false)
                .remove("token")
                .remove("accessToken")
                .remove("username")
                .remove("role")
                .apply();
        goLogin();
    }

    // ───────── FCM 토큰 획득 & 서버 전송 (검증 통과 후) ─────────
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
        } else { // teacher / director / parent (기존 흐름 유지)
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

    // ───────── 네트워크 유틸 ─────────
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && (
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            @SuppressWarnings("deprecation")
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        }
    }

    // ───────── 기타 ─────────
    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
