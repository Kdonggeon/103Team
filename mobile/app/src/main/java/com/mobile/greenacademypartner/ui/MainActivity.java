package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);

        if (!isLoggedIn) {
            // 로그인 안 됨 → LoginActivity로 이동
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // 로그인 되어있음 → 역할별 분기
        String role = prefs.getString("role", "student");
        Log.d("MainActivity", "로그인된 사용자 role: " + role);

        Intent intent;
        switch (role.toLowerCase()) {
            case "student":
                intent = new Intent(this, StudentTimetableActivity.class);
                break;
            case "teacher":
                intent = new Intent(this, TeacherTimetableActivity.class);
                break;
            case "parent":
                intent = new Intent(this, ParentChildrenListActivity.class);
                break;
            default:
                intent = new Intent(this, LoginActivity.class); // fallback
                break;
        }

        startActivity(intent);
        finish(); // 현재 MainActivity 종료
    }
}
