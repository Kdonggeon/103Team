package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;

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

            // 추후 서버 연동 전까지는 임시 하드코딩
            if (inputId.equals("admin") && inputPw.equals("1234")) {
                loginSuccess();
            } else {
                Toast.makeText(this, "아이디 또는 비밀번호가 틀렸습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 로그인 성공 처리
    private void loginSuccess() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("is_logged_in", true).apply();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
