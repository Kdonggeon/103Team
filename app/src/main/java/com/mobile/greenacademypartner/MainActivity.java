package com.mobile.greenacademypartner;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private LinearLayout selectedItem = null;

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

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setupNavigationMenu();
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
                // 이전 항목 스타일 초기화
                if (selectedItem != null) {
                    int prevIndex = getIndexOfView(selectedItem, navContainer);
                    ImageView prevIcon = selectedItem.findViewById(R.id.nav_icon);
                    TextView prevText = selectedItem.findViewById(R.id.nav_text);
                    prevIcon.setImageResource(icons_light[prevIndex]);
                    prevText.setTextColor(Color.BLACK);
                    selectedItem.setBackgroundColor(Color.LTGRAY);
                }

                // 현재 선택 항목 강조
                icon.setImageResource(icons_dark[index]);
                text.setTextColor(Color.WHITE);
                layout.setBackgroundColor(Color.DKGRAY);
                selectedItem = layout;

                Toast.makeText(this, labels[index] + " 클릭됨", Toast.LENGTH_SHORT).show();
                drawerLayout.closeDrawer(navContainer);
            });

            layout.setBackgroundColor(Color.LTGRAY); // 기본 회색
            navContainer.addView(itemView);
            // 초기 선택: "시간표"
            int defaultIndex = 2;
            View defaultView = navContainer.getChildAt(defaultIndex);
            if (defaultView != null) {
                defaultView.performClick(); // 강제로 클릭 이벤트 발생시켜서 선택
            }

        }
    }

    private int getIndexOfView(View view, ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i) == view) return i;
        }
        return -1;
    }

}
