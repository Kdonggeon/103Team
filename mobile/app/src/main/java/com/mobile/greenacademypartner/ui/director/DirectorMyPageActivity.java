//package com.mobile.greenacademypartner.ui.director;
//
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import androidx.appcompat.app.ActionBarDrawerToggle;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.Toolbar;
//import androidx.drawerlayout.widget.DrawerLayout;
//
//import com.mobile.greenacademypartner.R;
//import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
//import com.mobile.greenacademypartner.menu.ToolbarColorUtil; // 프로젝트에 이미 있으면 사용
//
//public class DirectorMyPageActivity extends AppCompatActivity {
//
//    private DrawerLayout drawerLayout;
//    private Toolbar toolbar;
//    private LinearLayout navContainer;
//
//    private TextView mainContentText;
//    private TextView tvDirectorName, tvDirectorId, tvDirectorPhone, tvAcademyNumber;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_director_mypage);
//
//        toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        drawerLayout = findViewById(R.id.drawer_layout);
//        navContainer = findViewById(R.id.nav_container);
//        mainContentText = findViewById(R.id.main_content_text);
//
//        // 사이드바 토글
//        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
//                this, drawerLayout, toolbar,
//                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
//        drawerLayout.addDrawerListener(toggle);
//        toggle.syncState();
//
//        // 툴바/테마 색상(기존 유틸 재사용)
//        try {
//            ToolbarColorUtil.applyToolbarColor(this, toolbar);
//        } catch (Exception ignore) {}
//
//        // ✅ 원장 메뉴로 세팅 (초기 선택: 0 = 마이페이지)
//        NavigationMenuHelper.setupMenu(
//                this,
//                navContainer,
//                drawerLayout,
//                mainContentText,
//                0,
//                NavigationMenuHelper.Role.DIRECTOR
//        );
//
//        // 본문 바인딩
//        tvDirectorName = findViewById(R.id.tvDirectorName);
//        tvDirectorId   = findViewById(R.id.tvDirectorId);
//        tvDirectorPhone= findViewById(R.id.tvDirectorPhone);
//        tvAcademyNumber= findViewById(R.id.tvAcademyNumber);
//
//        // ✅ SharedPreferences에서 로그인 정보 로딩 (프로젝트 키에 맞게 조정)
//        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
//        String role = prefs.getString("role", "");
//        String name = prefs.getString("name", "");
//        String username = prefs.getString("username", "");
//        String phone = prefs.getString("phone", "");
//        int academyNumber = prefs.getInt("academyNumber", 0);
//
//        // 표시
//        tvDirectorName.setText("이름: " + (name != null ? name : ""));
//        tvDirectorId.setText("아이디: " + (username != null ? username : ""));
//        tvDirectorPhone.setText("전화번호: " + (phone != null ? phone : ""));
//        tvAcademyNumber.setText("학원 번호: " + academyNumber);
//    }
//}
