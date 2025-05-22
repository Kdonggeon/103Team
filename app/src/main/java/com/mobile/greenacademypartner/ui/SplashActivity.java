package com.mobile.greenacademypartner.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 5초 뒤 MainActivity로 이동
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // 뒤로가기 눌러도 스플래시 화면이 안 보이게 종료
        }, 3000); // 5000ms = 5초
    }

    public static class MainActivity extends AppCompatActivity {

        private DrawerLayout drawerLayout;
        private Toolbar toolbar;
        private LinearLayout navContainer;
        private LinearLayout selectedItem = null;
        private TextView mainContentText;

        private String[] labels = {
                "My Page", "출석 관리", "시간표", "Q&A", "공지사항", "설정"
        };

        private int[] icons_light = {
                R.drawable.ic_person_light,
                R.drawable.ic_attendance_light,
                R.drawable.ic_timetable_light,
                R.drawable.ic_qa_light,
                R.drawable.ic_notice_light,
                R.drawable.ic_settings_light
        };

        private int[] icons_dark = {
                R.drawable.ic_person_dark,
                R.drawable.ic_attendance_dark,
                R.drawable.ic_timetable_dark,
                R.drawable.ic_qa_dark,
                R.drawable.ic_notice_dark,
                R.drawable.ic_settings_dark
        };

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            drawerLayout = findViewById(R.id.drawer_layout);
            toolbar = findViewById(R.id.toolbar);
            navContainer = findViewById(R.id.nav_container);
            mainContentText = findViewById(R.id.main_content_text);

            setSupportActionBar(toolbar);

            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close
            );
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            //메뉴 호출
            NavigationMenuHelper.setupMenu(this, navContainer);




            // 앱 시작 시 "시간표" 자동 선택
            int defaultIndex = 2;
            View defaultView = navContainer.getChildAt(defaultIndex);
            if (defaultView != null) {
                defaultView.performClick();
            }
        }


        private int getIndexOfView(View view, ViewGroup parent) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                if (parent.getChildAt(i) == view) return i;
            }
            return -1;
        }
    }
}
