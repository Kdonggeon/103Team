package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);  // XML 먼저 적용

        // 이제 뷰 초기화
        findAccount = findViewById(R.id.find_account);
        signupText = findViewById(R.id.signup_next);
        editTextId = findViewById(R.id.editTextId);           // XML에 아이디 입력칸
        editTextPassword = findViewById(R.id.editTextPassword); // 비밀번호 입력칸
        loginButton = findViewById(R.id.buttonLogin);         // 로그인 버튼

        // 회원가입 이동
        signupText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // 계정 찾기 이동
        findAccount.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, FindAccountActivity.class);
            startActivity(intent);
        });

        // 로그인 버튼 눌렀을 때
        loginButton.setOnClickListener(v -> {
            String inputId = editTextId.getText().toString().trim();
            String inputPw = editTextPassword.getText().toString().trim();

            LoginRequest request = new LoginRequest(inputId, inputPw);
            AuthApi api = RetrofitClient.getClient().create(AuthApi.class);

            api.login(request).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse res = response.body();

                        // JWT 토큰 SharedPreferences에 저장
                        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("is_logged_in", true)
                                .putString("token", res.getToken())
                                .putString("role", res.getRole())
                                .putString("username", res.getUsername())
                                .apply();

                        Toast.makeText(LoginActivity.this, res.getRole() + " 로그인 성공", Toast.LENGTH_SHORT).show();

                        // 홈 화면으로 이동
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "로그인 실패: 아이디 또는 비밀번호를 확인하세요", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override

                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Log.e("RetrofitError", "서버 연결 실패", t);  // 이 줄 추가
                    Toast.makeText(LoginActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
                }

            });
        });

    }



}
