package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.mobile.greenacademypartner.R;

public class SettingActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private GridLayout colorGrid;


    private int[] colorValues = {
            Color.parseColor("#65E478"), // 연두
            Color.BLACK,
            Color.RED,
            Color.rgb(218, 143, 57), // 갈색
            Color.YELLOW,
            Color.MAGENTA,
            Color.BLUE,
            Color.CYAN
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        drawerLayout = findViewById(R.id.drawer_layout_setting);
        toolbar = findViewById(R.id.toolbar_setting);
        colorGrid = findViewById(R.id.color_grid);

        // 툴바 설정 + 뒤로가기 버튼
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 색상 버튼 생성
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
                toolbar.setBackgroundColor(color); // 자기 툴바 변경
                getSharedPreferences("settings", MODE_PRIVATE)
                        .edit()
                        .putInt("toolbar_color", color)
                        .apply();
            });


            colorGrid.addView(colorBox);
        }
    }

    // 뒤로가기 버튼 기능
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}