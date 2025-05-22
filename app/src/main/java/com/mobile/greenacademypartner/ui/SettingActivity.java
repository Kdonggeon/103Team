package com.mobile.greenacademypartner.ui;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
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

public class SettingActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private TextView mainContentText; // optional
    private GridLayout colorGrid;

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
        mainContentText = findViewById(R.id.main_content_text); // 없으면 null 넘겨도 OK

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

        // ✅ 4. 메뉴 생성
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, mainContentText);

        // 5. 색상 선택 로직은 그대로 유지 (아까 쓰던 코드 사용)
        setupColorSelection();
        int settingIndex = 5; // "설정"이 6번째 항목
        View settingView = navContainer.getChildAt(settingIndex);
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
