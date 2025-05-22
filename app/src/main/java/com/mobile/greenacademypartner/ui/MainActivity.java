package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.ui.SettingActivity;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private TextView mainContentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navContainer = findViewById(R.id.nav_container);
        mainContentText = findViewById(R.id.main_content_text);

        // 툴바 설정
        setSupportActionBar(toolbar);

        // 저장된 색상 적용
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int savedColor = prefs.getInt("toolbar_color", getResources().getColor(R.color.green));
        toolbar.setBackgroundColor(savedColor);

        // 사이드 메뉴 토글
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 메뉴 구성
        NavigationMenuHelper.setupMenu(this, navContainer);

        // 메뉴 클릭 동작 설정
        for (int i = 0; i < navContainer.getChildCount(); i++) {
            View item = navContainer.getChildAt(i);
            int index = i;

            item.setOnClickListener(v -> {
                String label = NavigationMenuHelper.labels[index];

                if (label.equals("설정")) {
                    startActivity(new Intent(this, SettingActivity.class));
                } else {
                    mainContentText.setText(label + " 화면입니다");
                }

                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        // 앱 시작 시 "시간표" 선택
        int defaultIndex = 2;
        View defaultView = navContainer.getChildAt(defaultIndex);
        if (defaultView != null) {
            defaultView.performClick();
        }
    }
}
