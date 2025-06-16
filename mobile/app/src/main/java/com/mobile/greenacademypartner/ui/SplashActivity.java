package com.mobile.greenacademypartner.ui;

<<<<<<< HEAD
=======
import android.annotation.SuppressLint;
import android.app.Activity;
>>>>>>> sub
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
<<<<<<< HEAD

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500; // 1.5초 지연

=======
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

>>>>>>> sub
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
            boolean autoLogin = prefs.getBoolean("auto_login", false);

<<<<<<< HEAD
            Intent intent;

            if (isLoggedIn && autoLogin) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }

            startActivity(intent);
            finish(); // 반드시 호출
        }, SPLASH_DELAY);
    }
}
=======
            if (isLoggedIn && autoLogin) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }



            finish(); // 스플래시 종료
        }, 1500); // 1.5초 지연
    }
}

>>>>>>> sub
