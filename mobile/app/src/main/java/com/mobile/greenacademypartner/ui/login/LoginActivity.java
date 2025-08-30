package com.mobile.greenacademypartner.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.gson.Gson;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.AuthApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
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

        // 자동로그인 체크박스 초기상태
        autoLoginCheckBox.setChecked(prefs.getBoolean("auto_login", false));

        // 비밀번호 가시성 토글
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

            AuthApi authApi = RetrofitClient.getClient().create(AuthApi.class);
            authApi.login(new LoginRequest(inputId, inputPw))
                    .enqueue(new Callback<LoginResponse>() {
                        @Override
                        public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                LoginResponse res = response.body();
                                Log.d(TAG, "로그인 성공: " + new Gson().toJson(res));

                                // 필수값 정규화
                                String role = safeLower(res.getRole());          // "student" | "teacher" | "parent" | "director"
                                String username = safe(res.getUsername());
                                String token = safe(res.getToken());

                                if (username.isEmpty() || role.isEmpty() || token.isEmpty()) {
                                    Toast.makeText(LoginActivity.this, "로그인 응답이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("is_logged_in", true);
                                editor.putBoolean("auto_login", autoLoginCheckBox.isChecked());

                                // ★ 토큰 호환 저장 (MainActivity에서 token / accessToken 둘 다 읽을 수 있게)
                                editor.putString("token", token);
                                editor.putString("accessToken", token);

                                editor.putString("role", role);
                                editor.putString("username", username);

                                // 선택 정보
                                editor.putString("name", safe(res.getName()));
                                editor.putString("phone", safe(res.getPhone()));
                                editor.putString("userId", username); // 기존 호환

                                // 학생 전용 필드 (아니면 정리)
                                if ("student".equals(role)) {
                                    editor.putString("address", safe(res.getAddress()));
                                    editor.putString("school", safe(res.getSchool()));
                                    editor.putInt("grade", safeInt(res.getGrade())); // null → 0
                                    editor.putString("gender", safe(res.getGender()));
                                } else {
                                    editor.remove("address");
                                    editor.remove("school");
                                    editor.remove("grade");
                                    editor.remove("gender");
                                }

                                // 공통: academyNumbers(JSON 문자열로 저장)
                                List<Integer> academyNumbers = res.getAcademyNumbers();
                                editor.putString("academyNumbers",
                                        academyNumbers != null ? new JSONArray(academyNumbers).toString() : "[]");

                                editor.apply();

                                // 메인으로 이동
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Log.e(TAG, "응답 실패: code=" + response.code());
                                try {
                                    if (response.errorBody() != null) {
                                        Log.e(TAG, "에러 바디: " + response.errorBody().string());
                                    }
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
                            Toast.makeText(LoginActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    // ===== Helpers =====
    private String safe(String s) { return s == null ? "" : s.trim(); }
    private String safeLower(String s) { return safe(s).toLowerCase(); }
    private int safeInt(Integer v) { return v == null ? 0 : v; }
}
