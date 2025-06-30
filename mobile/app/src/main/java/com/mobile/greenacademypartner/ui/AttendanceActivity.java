package com.mobile.greenacademypartner.ui;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;

public class AttendanceActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private TextView mainContentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);  // XML 연결

        // 1. 뷰 연결 (ID는 XML과 일치)
        drawerLayout = findViewById(R.id.drawer_layout_attendance);
        toolbar = findViewById(R.id.toolbar_attendance);
        navContainer = findViewById(R.id.nav_container_attendance);
        mainContentText = findViewById(R.id.main_content_text_attendance);

        // 2. 툴바 색상 및 설정
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);

        // 3. 햄버거 메뉴 토글 설정
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 4. 사이드 메뉴 항목 구성
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, mainContentText);
    }
}
