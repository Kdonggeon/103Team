package com.mobile.greenacademypartner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 5초 뒤 MainActivity로 이동
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // 뒤로가기 눌러도 스플래시 화면이 안 보이게 종료
        }, 5000); // 5000ms = 5초
    }
}
