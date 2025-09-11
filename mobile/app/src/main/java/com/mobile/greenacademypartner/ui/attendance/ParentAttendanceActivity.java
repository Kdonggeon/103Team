package com.mobile.greenacademypartner.ui.attendance;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.attendance.AttendanceResponse;
import com.mobile.greenacademypartner.ui.adapter.AttendanceAdapter;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParentAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private RecyclerView attendanceListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_attendance);

        // ✅ 뷰 초기화
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        attendanceListView = findViewById(R.id.attendance_list_view);

        int white = androidx.core.content.ContextCompat.getColor(this, android.R.color.white);
        toolbar.setTitleTextColor(white);
        if (toolbar.getNavigationIcon() != null) toolbar.getNavigationIcon().setTint(white);
        if (toolbar.getOverflowIcon() != null) toolbar.getOverflowIcon().setTint(white);

        setupToolbarAndDrawer();
        ThemeColorUtil.applyThemeColor(this, toolbar);
        fetchChildAttendance();
    }

    private void setupToolbarAndDrawer() {
        setSupportActionBar(toolbar);
        setTitle("자녀 출석 확인");

        ToolbarColorUtil.applyToolbarColor(this, toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationMenuHelper.setupMenu(
                this,
                navContainer,
                drawerLayout,
                null,
                1 // 출석 메뉴 인덱스
        );
    }

    private void fetchChildAttendance() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String childId = prefs.getString("childStudentId", null);

        if (childId == null) {
            Toast.makeText(this, "자녀 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            Log.e("ParentAttendance", "❌ childStudentId is null");
            return;
        }

        // 부모 화면도 학생 API를 이용해 자녀(studentId) 출석을 조회
        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
        api.getAttendanceForStudent(childId).enqueue(new Callback<List<AttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AttendanceResponse> list = response.body();

                    // 🔢 날짜 오름차순(과거 → 최근) 정렬
                    Collections.sort(list, Comparator.comparing(AttendanceResponse::getDate, String::compareTo));

                    // ✅ RecyclerView 설정 (학원명/수업/날짜/상태 카드)
                    attendanceListView.setLayoutManager(new LinearLayoutManager(ParentAttendanceActivity.this));
                    AttendanceAdapter adapter = new AttendanceAdapter(ParentAttendanceActivity.this, list);
                    attendanceListView.setAdapter(adapter);

                    // 로그
                    for (AttendanceResponse att : list) {
                        Log.d("ParentAttendance",
                                "학원명=" + att.getAcademyName()
                                        + ", 수업명=" + att.getClassName()
                                        + ", 날짜=" + att.getDate()
                                        + ", 상태=" + att.getStatus());
                    }
                } else {
                    Toast.makeText(ParentAttendanceActivity.this, "출석 조회 실패", Toast.LENGTH_SHORT).show();
                    Log.e("ParentAttendance", "응답 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                Toast.makeText(ParentAttendanceActivity.this, "서버 오류 발생: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ParentAttendance", "API 호출 실패", t);
            }
        });
    }
}
