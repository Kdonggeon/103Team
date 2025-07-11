package com.mobile.greenacademypartner.ui.attendance;

import android.content.SharedPreferences;
import android.os.Build;
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
import com.mobile.greenacademypartner.model.attendance.Attendance;
import com.mobile.greenacademypartner.ui.adapter.ParentAttendanceAdapter;
import com.mobile.greenacademypartner.ui.setting.SettingActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.time.LocalDate;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChildAttendanceActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;

    private RecyclerView recyclerView;
    private ParentAttendanceAdapter adapter;
    private StudentApi api;
    private String studentId;
    private String today;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_attendance);

        // ✅ 툴바 설정
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("자녀 출석");

        // ✅ 드로어 설정
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // ✅ 사이드 메뉴 설정
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, 2);

        // ✅ RecyclerView 설정
        recyclerView = findViewById(R.id.recycler_today_attendance);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // ✅ SharedPreferences에서 studentId 가져오기
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        studentId = prefs.getString("childStudentId", null);

        if (studentId == null) {
            Toast.makeText(this, "자녀 정보가 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ✅ 오늘 날짜 구하기
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            today = LocalDate.now().toString();
        }

        // ✅ API 호출
        api = RetrofitClient.getClient().create(StudentApi.class);
        Call<List<Attendance>> call = api.getAttendanceByStudentIdAndDate(studentId, today);

        call.enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter = new ParentAttendanceAdapter(ChildAttendanceActivity.this, response.body());
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(ChildAttendanceActivity.this, "출석 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Attendance>> call, Throwable t) {
                Toast.makeText(ChildAttendanceActivity.this, "서버 통신 오류", Toast.LENGTH_SHORT).show();
                Log.e("ChildAttendance", "error: ", t);
            }
        });
        ThemeColorUtil.applyThemeColor(this, toolbar);
    }

}
