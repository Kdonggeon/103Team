package com.mobile.greenacademypartner.ui;

import android.os.Bundle;
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
}
