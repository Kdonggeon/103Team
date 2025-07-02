package com.mobile.greenacademypartner.ui.qa;

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

public class QAActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private TextView mainContentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qa);  // XML 파일명

        // 1. 뷰 연결
        drawerLayout = findViewById(R.id.drawer_layout_qa);
        toolbar = findViewById(R.id.toolbar_qa);
        navContainer = findViewById(R.id.nav_container_qa);
        mainContentText = findViewById(R.id.main_content_text);

        // 2. 툴바 설정
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);

        // 3. 햄버거 메뉴 토글 연결
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 4. NavigationMenuHelper로 메뉴 연결
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, mainContentText,4);
    }
}
