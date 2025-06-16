package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
<<<<<<< HEAD
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
=======
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private TextView mainContentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 뷰 초기화
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navContainer = findViewById(R.id.nav_container);
        mainContentText = findViewById(R.id.main_content_text);

        // 2. 툴바 색상 적용
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);

        // 3. 드로어 토글 연결
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // ✅ 4. 메뉴 생성 및 동작 연결 (NavigationMenuHelper에서 처리)
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, mainContentText);

        // 5. 시작 시 "시간표" 자동 선택
        int defaultIndex = 2;
        View defaultView = navContainer.getChildAt(defaultIndex);
        if (defaultView != null) defaultView.performClick();
    }
    private void logout() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("is_logged_in", false).apply();  // 로그인 해제

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // 현재 액티비티 종료
    }

>>>>>>> sub
}
