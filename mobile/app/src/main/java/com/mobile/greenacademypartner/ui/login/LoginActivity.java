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

    private TextView findAccount, signupText;
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

        // UI
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
                editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.eye_off);
            } else {
                editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.eye);
            }
            editTextPassword.setSelection(editTextPassword.length());
            isPasswordVisible = !isPasswordVisible;
        });

        signupText.setOnClickListener(v ->
                startActivity(new Intent(this, RoleSelectActivity.class)));
        findAccount.setOnClickListener(v ->
                startActivity(new Intent(this, FindSelectActivity.class)));

        // -------------------------
        // 로그인 버튼
        // -------------------------
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
                                LoginResponse res = response.body();
                                Log.d(TAG, "로그인 성공: " + new Gson().toJson(res));

                                String roleLower = safeLower(res.getRole());
                                String username  = safe(res.getUsername());
                                String jwt       = safe(res.getToken());

                                if (username.isEmpty() || roleLower.isEmpty() || jwt.isEmpty()) {
                                    Toast.makeText(LoginActivity.this, "로그인 응답이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // 🔥 로그인 정보 저장
                                mergeAndSaveLoginToPrefs(res, autoLoginCheckBox.isChecked());

                                // 부모는 자녀 선택 초기화
                                if ("parent".equals(roleLower)) {
                                    prefs.edit()
                                            .remove("selected_child")
                                            .remove("selected_child_id")
                                            .remove("selected_academy_number")
                                            .apply();
                                }

                                // 학생 → studentId 저장
                                if ("student".equals(roleLower)) {
                                    prefs.edit()
                                            .putString("student_id", username)
                                            .putString("token", jwt)
                                            .apply();
                                }

                                // FCM 업데이트
                                new Handler(Looper.getMainLooper()).postDelayed(() ->
                                        upsertFcmTokenImmediately(roleLower, username), 800);

                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.e(TAG, "로그인 실패: code=" + response.code());
                                try {
                                    Log.e(TAG, "에러 바디: " + response.errorBody().string());
                                } catch (IOException ignored) {}

                                Toast.makeText(LoginActivity.this,
                                        "로그인 실패: 아이디 또는 비밀번호를 확인하세요",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<LoginResponse> call, Throwable t) {
                            Log.e(TAG, "서버 연결 실패", t);
                            Toast.makeText(LoginActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    // ---------------------------------------------------------------------
    // 🔥 로그인 정보 저장 (수정된 부분 포함)
    // ---------------------------------------------------------------------
    private void mergeAndSaveLoginToPrefs(LoginResponse res, boolean autoLoginChecked) {

        SharedPreferences.Editor ed = prefs.edit();

        ed.putBoolean("is_logged_in", true);
        ed.putBoolean("auto_login", autoLoginChecked);
        ed.putString("token", safe(res.getToken()));
        ed.putString("role", safeLower(res.getRole()));
        ed.putString("username", safe(res.getUsername()));
        ed.putString("userId", safe(res.getUsername()));

        // 🔥 MainActivity가 읽는 이름 키
        ed.putString("student_name", safe(res.getName()));  // ★ 추가됨 → 학생 이름 정상 표시

        // 기존 name도 유지 (다른 화면에서 사용 가능)
        ed.putString("name", safe(res.getName()));

        ed.putString("phone", safe(res.getPhone()));
        ed.putString("address", safe(res.getAddress()));
        ed.putString("school", safe(res.getSchool()));
        ed.putString("gender", safe(res.getGender()));
        ed.putInt("grade", res.getGrade());

        // 학부모용 필드
        if ("parent".equalsIgnoreCase(safeLower(res.getRole()))) {
            ed.putString("parentsNumber", safe(res.getParentsNumber()));
            ed.putString("childStudentId", safe(res.getChildStudentId()));
        }

        // 학원 번호
        List<Integer> academyNumbers = res.getAcademyNumbers();
        ed.putString(
                "academyNumbers",
                academyNumbers != null ? new JSONArray(academyNumbers).toString() : "[]"
        );

        if (academyNumbers != null && !academyNumbers.isEmpty()) {
            ed.putString("academy_numbers_json", new JSONArray(academyNumbers).toString());
            ed.putString("academy_numbers", academyNumbers.toString());
            ed.putInt("academyNumber", academyNumbers.get(0));
        } else {
            ed.putString("academy_numbers_json", "[]");
            ed.putString("academy_numbers", "");
        }

        ed.commit();
    }

    // ---------------------------------------------------------------------
    // 유틸
    // ---------------------------------------------------------------------

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
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    // ---------------------------------------------------------------------
    // FCM 업서트
    // ---------------------------------------------------------------------
    private void upsertFcmTokenImmediately(String roleLower, String username) {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (token == null || token.trim().isEmpty()) {
                Log.w(TAG, "FCM 토큰이 비어 있습니다.");
                return;
            }

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
        });
    }

    private static class VoidLoggingCallback implements Callback<Void> {
        private final String tagSuffix;
        private VoidLoggingCallback(String tagSuffix) { this.tagSuffix = tagSuffix; }

        @Override
        public void onResponse(Call<Void> call, Response<Void> response) {
            if (response.isSuccessful()) {
                Log.d(TAG, "✅ FCM 업서트 성공(" + tagSuffix + ")");
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
