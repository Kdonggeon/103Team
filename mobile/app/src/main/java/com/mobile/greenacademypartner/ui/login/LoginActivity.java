package com.mobile.greenacademypartner.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.AuthApi;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.model.login.LoginRequest;
import com.mobile.greenacademypartner.model.login.LoginResponse;
import com.mobile.greenacademypartner.ui.main.MainActivity;

import org.json.JSONArray;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "login_prefs";


    private TextView findAccount;
    private TextView signupText;
    private EditText editTextId, editTextPassword;
    private Button loginButton;
    private CheckBox autoLoginCheckBox;
    private ImageView btnTogglePassword;
    private boolean isPasswordVisible = false;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 뷰 바인딩
        findAccount = findViewById(R.id.find_account);
        signupText = findViewById(R.id.signup_next);
        editTextId = findViewById(R.id.editTextId);
        editTextPassword = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        autoLoginCheckBox = findViewById(R.id.login_check);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);

        // 자동로그인 체크박스 초기값
        autoLoginCheckBox.setChecked(prefs.getBoolean("auto_login", false));


        // 알림 권한(Android 13+) 사전 요청
        requestNotificationPermissionIfNeeded();

        // 비밀번호 토글
        btnTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.eye_off);
            } else {
                editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.eye);
            }
            editTextPassword.setSelection(editTextPassword.length());
            isPasswordVisible = !isPasswordVisible;
        });

        // 회원가입 / 계정찾기 이동
        signupText.setOnClickListener(v -> startActivity(new Intent(this, RoleSelectActivity.class)));
        findAccount.setOnClickListener(v -> startActivity(new Intent(this, FindSelectActivity.class)));

        // 로그인 처리
        loginButton.setOnClickListener(v -> {
            String inputId = safe(editTextId.getText().toString());
            String inputPw = safe(editTextPassword.getText().toString());

            if (inputId.isEmpty() || inputPw.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔒 네트워크 연결 체크: 없으면 즉시 종료
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "네트워크 연결이 없습니다. 연결 후 다시 시도하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            AuthApi authApi = RetrofitClient.getClient().create(AuthApi.class);
            authApi.login(new LoginRequest(inputId, inputPw))
                    .enqueue(new Callback<LoginResponse>() {
                        @Override
                        public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {

                                Log.d(TAG, "로그인 성공: " + new Gson().toJson(response.body()));

                                LoginResponse res = response.body();
                                Log.d(TAG, "로그인 성공: " + new Gson().toJson(res));

                                String role = safeLower(res.getRole());
                                String username = safe(res.getUsername());
                                String token = safe(res.getToken());

                                if (username.isEmpty() || role.isEmpty() || token.isEmpty()) {
                                    Toast.makeText(LoginActivity.this, "로그인 응답이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("is_logged_in", true);
                                editor.putBoolean("auto_login", autoLoginCheckBox.isChecked());
                                editor.putString("token", res.getToken());
                                editor.putString("role", res.getRole().toLowerCase());
                                editor.putString("username", res.getUsername());
                                editor.putString("name", res.getName());
                                editor.putString("phone", res.getPhone());
                                // 공통 userId(기존 호환)
                                editor.putString("userId", res.getUsername());

                                // 역할별 ID를 명시적으로 저장(다른 역할 키는 정리)
                                if ("teacher".equals(role) || "director".equals(role)) {
                                    editor.putString("teacherId", res.getUsername());
                                    editor.remove("studentId");
                                    editor.remove("parentId");
                                } else if ("student".equals(role)) {
                                    editor.putString("studentId", res.getUsername());
                                    editor.remove("teacherId");
                                    editor.remove("parentId");
                                } else if ("parent".equals(role)) {
                                    editor.putString("parentId", res.getUsername());
                                    editor.remove("teacherId");
                                    editor.remove("studentId");
                                } else {
                                    editor.remove("teacherId");
                                    editor.remove("studentId");
                                    editor.remove("parentId");
                                }

                                // 학생 역할 부가 정보
                                if ("student".equals(role)) {
                                    editor.putString("address", res.getAddress());
                                    editor.putString("school", res.getSchool());
                                    editor.putInt("grade", res.getGrade());
                                    editor.putString("gender", res.getGender());
                                } else {
                                    editor.remove("address");
                                    editor.remove("school");
                                    editor.remove("grade");
                                    editor.remove("gender");
                                }

                                editor.putString("role", role);
                                editor.putString("username", username);

                                // 선택 정보
                                editor.putString("name", safe(res.getName()));
                                editor.putString("phone", safe(res.getPhone()));
                                editor.putString("userId", username); // 호환 키

                                // 학생 전용 필드
                                if ("student".equals(role)) {
                                    editor.putString("address", safe(res.getAddress()));
                                    editor.putString("school", safe(res.getSchool()));
                                    editor.putInt("grade", safeInt(res.getGrade())); // null -> 0
                                    editor.putString("gender", safe(res.getGender()));
                                } else {
                                    editor.remove("address");
                                    editor.remove("school");
                                    editor.remove("grade");
                                    editor.remove("gender");
                                }

                                // 공통: academyNumbers(JSON)
                                List<Integer> academyNumbers = res.getAcademyNumbers();
                                editor.putString("academyNumbers",
                                        academyNumbers != null ? new JSONArray(academyNumbers).toString() : "[]");

                                editor.apply();

                                // ★★★ 로그인 직후 FCM 토큰 업서트 (세션 쿠키 포함된 RetrofitClient 사용) ★★★
                                upsertFcmTokenImmediately(role, res.getUsername());

                                // 역할 전환 시 상태 꼬임 방지: 태스크 초기화 후 진입
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {

                                Log.e(TAG, "응답 실패: code = " + response.code());
                                try {
                                    Log.e(TAG, "에러 바디: " + response.errorBody().string());
                                } catch (IOException e) {
                                    Log.e(TAG, "에러 바디 파싱 실패", e);
                                }
                                Toast.makeText(LoginActivity.this,
                                        "로그인 실패: 아이디 또는 비밀번호를 확인하세요",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<LoginResponse> call, Throwable t) {
                            Log.e(TAG, "서버 연결 실패", t);

                            Toast.makeText(LoginActivity.this,
                                    "서버 연결 실패",
                                    Toast.LENGTH_SHORT).show();

                        }
                    });
        });
    }

    // ===== Network Check =====
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps == null) return false;
            // Wi-Fi / 셀룰러 / 이더넷 중 하나라도 연결되어 있으면 OK
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
        } else {
            // Legacy
            NetworkInfo active = cm.getActiveNetworkInfo();
            return active != null && active.isConnected();
        }
    }

    // ===== Helpers =====
    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String safeLower(String s) {
        return safe(s).toLowerCase();
    }

    private int safeInt(Integer v) {
        return v == null ? 0 : v;
    }
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    /**
     * 로그인 직후, 토큰이 갱신되지 않았더라도 강제로 서버에 업서트.
     * 서버는 세션 기반(JSESSIONID)으로 userId/role을 식별하거나,
     * 역할별 API(updateFcmToken)로 userId를 명시 전달하는 구조를 사용.
     */
    private void upsertFcmTokenImmediately(String roleLower, String username) {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (token == null || token.trim().isEmpty()) {
                Log.w(TAG, "FCM 토큰이 비어 있습니다.");
                return;
            }
            Log.d(TAG, "FCM 토큰 획득: " + token + " / role=" + roleLower + ", user=" + username);

            // 역할별 엔드포인트로 업서트 (프로젝트에 구현된 API 사용)
            try {
                if ("parent".equals(roleLower)) {
                    ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
                    // 예: @POST("/api/parents/{id}/fcm-token") Call<Void> updateFcmToken(@Path("id") String parentsId, @Body String token);
                    api.updateFcmToken(username, token).enqueue(new VoidLoggingCallback("parent"));

                } else if ("student".equals(roleLower)) {
                    StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
                    // 예: @POST("/api/students/{id}/fcm-token")
                    api.updateFcmToken(username, token).enqueue(new VoidLoggingCallback("student"));

                } else {
                    Log.w(TAG, "알 수 없는 역할. FCM 토큰 업서트 생략: " + roleLower);
                }
            } catch (Exception e) {
                // 만약 역할별 API가 없다면, 공용 TokenApi(/api/fcm/token) 사용하도록 별도 인터페이스를 추가하세요.
                Log.e(TAG, "FCM 토큰 업서트 중 예외", e);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "FCM 토큰 획득 실패", e));
    }

    // 업서트 콜백 로깅용
    private static class VoidLoggingCallback implements Callback<Void> {
        private final String tagSuffix;
        private VoidLoggingCallback(String tagSuffix) { this.tagSuffix = tagSuffix; }

        @Override public void onResponse(Call<Void> call, Response<Void> response) {
            if (response.isSuccessful()) {
                Log.d(TAG, "FCM 토큰 업서트 성공(" + tagSuffix + ")");
            } else {
                Log.e(TAG, "FCM 토큰 업서트 실패(" + tagSuffix + "): code=" + response.code());
            }
        }
        @Override public void onFailure(Call<Void> call, Throwable t) {
            Log.e(TAG, "FCM 토큰 업서트 네트워크 실패(" + tagSuffix + ")", t);
        }
    }
}
