package com.mobile.greenacademypartner.ui.setting;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.ui.login.LoginActivity;

public class SettingActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private TextView mainContentText;
    private GridLayout colorGrid;
    private Button btnLogout;

    int defaultIndex = 5;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // 1. 뷰 초기화
        drawerLayout = findViewById(R.id.drawer_layout_setting);
        toolbar = findViewById(R.id.toolbar_setting);
        navContainer = findViewById(R.id.nav_container_setting);
        colorGrid = findViewById(R.id.color_grid);
        mainContentText = findViewById(R.id.main_content_text);
        btnLogout = findViewById(R.id.btn_logout);

        // 2. 툴바 색상 적용
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);

        // 3. 드로어 토글
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 4. 메뉴 생성
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, mainContentText, defaultIndex);

        // 5. 색상 선택
        setupColorSelection();

        // 6. 로그아웃 버튼 동작
        btnLogout.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            prefs.edit().clear().apply();

            Intent intent = new Intent(SettingActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 7. 설정 선택 상태로 표시
        View settingView = navContainer.getChildAt(defaultIndex);
        if (settingView != null) settingView.performClick();
    }

    private void setupColorSelection() {
        int[] colorValues = {
                getColor(R.color.green),
                Color.BLACK,
                Color.RED,
                Color.rgb(218, 143, 57),
                Color.YELLOW,
                Color.MAGENTA,
                Color.BLUE,
                Color.CYAN
        };

        for (int color : colorValues) {
            View colorBox = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 150;
            params.height = 150;
            params.setMargins(16, 16, 16, 16);
            colorBox.setLayoutParams(params);
            colorBox.setBackgroundColor(color);
            colorBox.setClickable(true);

            colorBox.setOnClickListener(v -> {
                toolbar.setBackgroundColor(color);
                getSharedPreferences("settings", MODE_PRIVATE)
                        .edit()
                        .putInt("toolbar_color", color)
                        .apply();
            });

            colorGrid.addView(colorBox);
        }
    }
}
