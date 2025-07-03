package com.mobile.greenacademypartner.ui.attendance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.ui.classes.TeacherClassesActivity;

public class AttendanceActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  반드시 레이아웃을 지정해줘야 화면이 뜹니다.
        setContentView(R.layout.activity_attendance);

        //  역할에 따라 분기
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", "");

        if ("student".equals(role)) {
            startActivity(new Intent(this, StudentAttendanceActivity.class));
        } else if ("teacher".equals(role)) {
            startActivity(new Intent(this, TeacherClassesActivity.class));
        } else if ("parent".equals(role)) {
            startActivity(new Intent(this, ParentAttendanceActivity.class));
        }

        finish(); // 이 액티비티는 중간 연결용
    }
}
