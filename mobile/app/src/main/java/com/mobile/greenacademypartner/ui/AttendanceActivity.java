package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class AttendanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String role = prefs.getString("role", "");

        if (role.equals("student")) {
            startActivity(new Intent(this, StudentAttendanceActivity.class));
        } else if (role.equals("teacher")) {
            startActivity(new Intent(this, TeacherAttendanceActivity.class));
        } else if (role.equals("parent")) {
            startActivity(new Intent(this, ParentAttendanceActivity.class));
        } else {
            // 잘못된 역할 또는 로그인 정보 없음
            finish(); // 그냥 종료
        }

        finish(); // 본 화면 종료
    }
}
