package com.mobile.greenacademypartner;

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

public class MainActivity extends AppCompatActivity {

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


        setupNavigationMenu();

        // 앱 시작 시 "시간표" 자동 선택
        int defaultIndex = 2;
        View defaultView = navContainer.getChildAt(defaultIndex);
        if (defaultView != null) {
            defaultView.performClick();
        }
    }

    private void setupNavigationMenu() {
        for (int i = 0; i < labels.length; i++) {
            View itemView = getLayoutInflater().inflate(R.layout.nav_drawer_item, navContainer, false);

            ImageView icon = itemView.findViewById(R.id.nav_icon);
            TextView text = itemView.findViewById(R.id.nav_text);
            LinearLayout layout = itemView.findViewById(R.id.nav_item_layout);

            icon.setImageResource(icons_light[i]);
            text.setText(labels[i]);

            int index = i;

            layout.setOnClickListener(v -> {
                if (selectedItem == layout) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    // drawerLayout.closeDrawer(navContainer); 를 쓰면 죽어서 변경 20250521
                    return;
                }

                // 기존 선택 해제
                if (selectedItem != null) {
                    int prevIndex = getIndexOfView(selectedItem, navContainer);
                    ImageView prevIcon = selectedItem.findViewById(R.id.nav_icon);
                    TextView prevText = selectedItem.findViewById(R.id.nav_text);
                    prevIcon.setImageResource(icons_light[prevIndex]);
                    prevText.setTextColor(Color.BLACK);
                    selectedItem.setBackgroundColor(Color.LTGRAY);
                }

                // 현재 선택 적용
                icon.setImageResource(icons_dark[index]);
                text.setTextColor(Color.WHITE);
                layout.setBackgroundColor(Color.DKGRAY);
                selectedItem = layout;

                // 메인 화면 내용 변경
                if (labels[index].equals("시간표")) {
                    mainContentText.setText("시간표 화면입니다");
                } else {
                    mainContentText.setText(labels[index] + "는 아직 구현되지 않았습니다.");
                }

                drawerLayout.closeDrawer(GravityCompat.START);
            });

            layout.setBackgroundColor(Color.LTGRAY);
            navContainer.addView(itemView);
        }
    }

    private int getIndexOfView(View view, ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i) == view) return i;
        }
        return -1;
    }
}
