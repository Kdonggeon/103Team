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
        prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);

        // 뷰 초기화
        findAccount = findViewById(R.id.find_account);
        signupText = findViewById(R.id.signup_next);
        editTextId = findViewById(R.id.editTextId);
        editTextPassword = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        autoLoginCheckBox = findViewById(R.id.login_check);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        autoLoginCheckBox.setChecked(prefs.getBoolean("auto_login", false));

        // 비밀번호 토글
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

        // 회원가입 / 계정 찾기 이동
        signupText.setOnClickListener(v ->
                startActivity(new Intent(this, RoleSelectActivity.class))
        );
        findAccount.setOnClickListener(v ->
                startActivity(new Intent(this, FindSelectActivity.class))
        );

        // 로그인 처리
        loginButton.setOnClickListener(v -> {
            String inputId = editTextId.getText().toString().trim();
            String inputPw = editTextPassword.getText().toString().trim();

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
                                Log.d("Login", "성공: " + new Gson().toJson(response.body()));
                                LoginResponse res = response.body();

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
                                String role = res.getRole() == null ? "" : res.getRole().toLowerCase();
                                if ("teacher".equals(role) || "director".equals(role)) {
                                    // 서버 응답에 별도 teacherId가 없으면 username을 교사 ID로 사용
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
                                    // 알 수 없는 역할 방어
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

                                // academyNumbers 공통 처리 (student/teacher/parent)
                                List<Integer> academyNumbers = res.getAcademyNumbers();
                                if (academyNumbers != null) {
                                    JSONArray jsonArray = new JSONArray(academyNumbers);
                                    editor.putString("academyNumbers", jsonArray.toString());
                                } else {
                                    editor.putString("academyNumbers", "[]");
                                }

                                editor.apply();

                                // 역할 전환 시 상태 꼬임 방지: 태스크 초기화 후 진입
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.e("Login", "응답 실패: code = " + response.code());
                                try {
                                    Log.e("Login", "에러 바디: " + response.errorBody().string());
                                } catch (IOException e) {
                                    Log.e("Login", "에러 바디 파싱 실패", e);
                                }

                                Toast.makeText(LoginActivity.this,
                                        "로그인 실패: 아이디 또는 비밀번호를 확인하세요",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<LoginResponse> call, Throwable t) {
                            Log.e("Login", "서버 연결 실패", t);
                            Toast.makeText(LoginActivity.this,
                                    "서버 연결 실패",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
