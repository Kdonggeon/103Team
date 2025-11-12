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
import android.os.Handler;
import android.os.Looper;
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
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        boolean autoLogin = prefs.getBoolean("auto_login", false);

        if (isLoggedIn && autoLogin) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        findAccount = findViewById(R.id.find_account);
        signupText = findViewById(R.id.signup_next);
        editTextId = findViewById(R.id.editTextId);
        editTextPassword = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        autoLoginCheckBox = findViewById(R.id.login_check);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);

        autoLoginCheckBox.setChecked(autoLogin);
        requestNotificationPermissionIfNeeded();

        btnTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                editTextPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                );
                btnTogglePassword.setImageResource(R.drawable.eye_off);
            } else {
                editTextPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );
                btnTogglePassword.setImageResource(R.drawable.eye);
            }
            editTextPassword.setSelection(editTextPassword.length());
            isPasswordVisible = !isPasswordVisible;
        });

        signupText.setOnClickListener(v ->
                startActivity(new Intent(this, RoleSelectActivity.class)));
        findAccount.setOnClickListener(v ->
                startActivity(new Intent(this, FindSelectActivity.class)));

        loginButton.setOnClickListener(v -> {
            String inputId = safe(editTextId.getText().toString());
            String inputPw = safe(editTextPassword.getText().toString());

            if (inputId.isEmpty() || inputPw.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isNetworkAvailable()) {
                Toast.makeText(this, "네트워크 연결이 없습니다.", Toast.LENGTH_SHORT).show();
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
                                String roleLower = safeLower(res.getRole());
                                String username  = safe(res.getUsername());
                                String jwt       = safe(res.getToken());

                                if (username.isEmpty() || roleLower.isEmpty() || jwt.isEmpty()) {
                                    Toast.makeText(LoginActivity.this,
                                            "로그인 응답이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // ✅ 1. 로그인 정보 저장
                                mergeAndSaveLoginToPrefs(res, autoLoginCheckBox.isChecked());

                                // ✅ 2. 학부모 로그인 시 이전 자녀/학원 정보 완전 초기화
                                if ("parent".equalsIgnoreCase(roleLower)) {
                                    SharedPreferences.Editor clearEditor = prefs.edit();
                                    clearEditor.remove("selected_child");
                                    clearEditor.remove("selected_child_id");
                                    clearEditor.remove("selected_academy_number");
                                    clearEditor.remove("academy_numbers_json");
                                    clearEditor.remove("academy_numbers");
                                    clearEditor.apply();
                                    Log.d(TAG, "🧹 학부모 로그인 시 이전 자녀/학원 정보 초기화 완료");
                                }

                                // ✅ 3. QRScannerActivity용 학생 정보 저장
                                if ("student".equalsIgnoreCase(roleLower)) {
                                    SharedPreferences loginPrefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
                                    loginPrefs.edit()
                                            .putString("student_id", res.getUsername())
                                            .putString("token", res.getToken())
                                            .apply();
                                    Log.d(TAG, "✅ QR 스캐너용 student_id/token 저장 완료");
                                }

                                // ✅ 4. FCM 토큰 업서트
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    upsertFcmTokenImmediately(roleLower, username);
                                }, 800);

                                // ✅ 5. 메인 화면 이동
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();

                            } else {
                                Log.e(TAG, "로그인 실패: code=" + response.code());
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

    // ✅ SharedPreferences 병합 + 학원번호 추가 저장
    private void mergeAndSaveLoginToPrefs(LoginResponse res, boolean autoLoginChecked) {
        SharedPreferences.Editor ed = prefs.edit();

        ed.putBoolean("is_logged_in", true);
        ed.putBoolean("auto_login", autoLoginChecked);
        ed.putString("token", safe(res.getToken()));
        ed.putString("role", safeLower(res.getRole()));
        ed.putString("username", safe(res.getUsername()));
        ed.putString("userId", safe(res.getUsername()));
        ed.putString("name", safe(res.getName()));
        ed.putString("phone", safe(res.getPhone()));
        ed.putString("address", safe(res.getAddress()));
        ed.putString("school", safe(res.getSchool()));
        ed.putString("gender", safe(res.getGender()));
        ed.putInt("grade", res.getGrade());

        List<Integer> academyNumbers = res.getAcademyNumbers();
        ed.putString(
                "academyNumbers",
                academyNumbers != null ? new JSONArray(academyNumbers).toString() : "[]"
        );

        if ("student".equalsIgnoreCase(safeLower(res.getRole()))) {
            ed.putString("student_name", safe(res.getName()));
        }

        if (academyNumbers != null && !academyNumbers.isEmpty()) {
            String json = new JSONArray(academyNumbers).toString();
            String csv = academyNumbers.toString().replaceAll("\\[|\\]|\\s", "");
            ed.putString("academy_numbers_json", json);
            ed.putString("academy_numbers", csv);
            ed.putInt("academyNumber", academyNumbers.get(0));
            Log.d(TAG, "✅ 학원번호 저장 완료: " + json);
        } else {
            ed.putString("academy_numbers_json", "[]");
            ed.putString("academy_numbers", "");
            ed.remove("academyNumber");
            Log.w(TAG, "⚠️ 학원번호 없음 → 기본값 저장");
        }

        ed.commit();
        Log.d(TAG, "[mergeAndSaveLoginToPrefs] 최종 저장 완료");
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
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
            NetworkInfo active = cm.getActiveNetworkInfo();
            return active != null && active.isConnected();
        }
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
    private String safeLower(String s) { return safe(s).toLowerCase(); }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }
    }

    private void upsertFcmTokenImmediately(String roleLower, String username) {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (token == null || token.trim().isEmpty()) {
                Log.w(TAG, "FCM 토큰이 비어 있습니다.");
                return;
            }
            Log.d(TAG, "FCM 토큰 획득: " + token);

            String rawJwt = prefs.getString("token", null);
            if (rawJwt == null || rawJwt.trim().isEmpty()) {
                Log.w(TAG, "JWT 없음 → FCM 업서트 생략");
                return;
            }
            String authHeader = "Bearer " + rawJwt.trim();

            try {
                if ("student".equalsIgnoreCase(roleLower)) {
                    StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
                    api.updateFcmToken(username, authHeader, token)
                            .enqueue(new VoidLoggingCallback("student"));
                } else if ("parent".equalsIgnoreCase(roleLower)) {
                    ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
                    api.updateFcmToken(username, authHeader, token)
                            .enqueue(new VoidLoggingCallback("parent"));
                }
            } catch (Exception e) {
                Log.e(TAG, "FCM 업서트 중 예외", e);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "FCM 토큰 획득 실패", e));
    }

    private static class VoidLoggingCallback implements Callback<Void> {
        private final String tagSuffix;
        private VoidLoggingCallback(String tagSuffix) { this.tagSuffix = tagSuffix; }

        @Override
        public void onResponse(Call<Void> call, Response<Void> response) {
            if (response.isSuccessful()) {
                Log.d(TAG, "✅ FCM 토큰 업서트 성공(" + tagSuffix + ")");
            } else {
                Log.e(TAG, "❌ FCM 업서트 실패(" + tagSuffix + "): code=" + response.code());
            }
        }

        @Override
        public void onFailure(Call<Void> call, Throwable t) {
            Log.e(TAG, "FCM 업서트 네트워크 실패(" + tagSuffix + ")", t);
        }
    }
}
