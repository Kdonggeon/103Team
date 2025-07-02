package com.mobile.greenacademypartner.ui.start;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.ui.login.LoginActivity;
import com.mobile.greenacademypartner.ui.main.MainActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500; // 1.5초 지연

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
            boolean autoLogin = prefs.getBoolean("auto_login", false);

            Intent intent;
            if (isLoggedIn && autoLogin) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }

            startActivity(intent);
            finish(); // 스플래시 액티비티 종료
        }, SPLASH_DELAY);
    }
}
