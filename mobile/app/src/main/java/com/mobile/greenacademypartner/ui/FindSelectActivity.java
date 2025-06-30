package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.mobile.greenacademypartner.R;

public class FindSelectActivity extends AppCompatActivity {

    private Button btnFindId, btnResetPw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_select);  // 새로 만든 xml

        btnFindId = findViewById(R.id.btn_find_id);
        btnResetPw = findViewById(R.id.btn_reset_pw);

        btnFindId.setOnClickListener(v -> {
            startActivity(new Intent(this, FindIdActivity.class));  // 아이디 찾기 화면으로 이동
        });

        btnResetPw.setOnClickListener(v -> {
            startActivity(new Intent(this, ResetPasswordActivity.class));  // 비밀번호 변경 화면으로 이동
        });
    }
}
