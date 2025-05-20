package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;

public class LoginActivity extends AppCompatActivity {
    TextView findAccount = findViewById(R.id.find_account);
    TextView signupText = findViewById(R.id.signup_next);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // ← 여기에 XML 파일 이름 넣기



        signupText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
        findAccount.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, FindAccountActivity.class);
            startActivity(intent);
        });
    }
}
