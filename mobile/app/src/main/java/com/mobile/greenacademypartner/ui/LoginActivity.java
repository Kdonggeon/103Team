package com.mobile.greenacademypartner.ui;

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
import com.mobile.greenacademypartner.model.LoginRequest;
import com.mobile.greenacademypartner.model.LoginResponse;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 뷰 초기화
        findAccount = findViewById(R.id.find_account);
        signupText = findViewById(R.id.signup_next);
        editTextId = findViewById(R.id.editTextId);
        editTextPassword = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        autoLoginCheckBox = findViewById(R.id.login_check);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);

        // 이전 체크 상태 유지 (자동 로그인 체크박스)
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        autoLoginCheckBox.setChecked(prefs.getBoolean("auto_login", false));

        // 눈 아이콘으로 비밀번호 표시/숨김
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

        // 회원가입 이동
        signupText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RoleSelectActivity.class));
        });

        // 계정 찾기 이동
        findAccount.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, FindSelectActivity.class));
        });

        // 로그인 처리
        loginButton.setOnClickListener(v -> {
            String inputId = editTextId.getText().toString().trim();
            String inputPw = editTextPassword.getText().toString().trim();

            if (inputId.isEmpty() || inputPw.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            LoginRequest request = new LoginRequest(inputId, inputPw);
            AuthApi api = RetrofitClient.getClient().create(AuthApi.class);

            Log.d("LoginData", "입력 아이디: " + inputId + ", 입력 비번: " + inputPw);

            api.login(request).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    Log.d("LoginResponse", "isSuccessful: " + response.isSuccessful());
                    Log.d("LoginResponse", "HTTP Code: " + response.code());

                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            LoginResponse res = response.body();

                            Log.d("LoginResponse", "응답 바디: " + new Gson().toJson(res));

                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("is_logged_in", true);
                            editor.putString("token", res.getToken());
                            editor.putString("role", res.getRole());
                            editor.putString("username", res.getUsername());
                            editor.putBoolean("auto_login", autoLoginCheckBox.isChecked());
                            editor.apply();

//                            Toast.makeText(LoginActivity.this, res.getRole() + " 로그인 성공", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "없음";
                            Log.e("LoginResponse", "실패 바디: " + errorBody);
                            Toast.makeText(LoginActivity.this, "로그인 실패: 아이디 또는 비밀번호를 확인하세요", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("LoginResponse", "응답 처리 중 예외 발생", e);
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Log.e("RetrofitError", "서버 연결 실패", t);
                    Toast.makeText(LoginActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
