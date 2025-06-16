package com.mobile.greenacademypartner.ui;

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
import com.mobile.greenacademypartner.model.Attendance;
import com.mobile.greenacademypartner.ui.adapter.AttendanceAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private RecyclerView attendanceListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        // ✅ 뷰 초기화
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        attendanceListView = findViewById(R.id.attendance_list_view);

        setupToolbarAndDrawer(); // 🛠️ 사이드바 및 툴바 설정
        fetchAttendanceFromServer(); // 📡 출석 데이터 로드
    }

    private void setupToolbarAndDrawer() {
        setTitle("출석 관리");
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, 1);
    }

    private void fetchAttendanceFromServer() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String studentId = prefs.getString("username", null);

        if (studentId == null) {
            Toast.makeText(this, "로그인 정보가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
        api.getAttendanceForStudent(studentId).enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Attendance> list = response.body();
                    Log.d("Attendance", "출석 데이터 개수: " + list.size());

                    // ✅ 리사이클러뷰 세팅
                    attendanceListView.setLayoutManager(new LinearLayoutManager(StudentAttendanceActivity.this));
                    AttendanceAdapter adapter = new AttendanceAdapter(StudentAttendanceActivity.this, list);
                    attendanceListView.setAdapter(adapter);

                    for (Attendance att : list) {
                        Log.d("Attendance", "수업명: " + att.getClassName() + ", 날짜: " + att.getDate() + ", 상태: " + att.getStatus());
                    }
                } else {
                    Toast.makeText(StudentAttendanceActivity.this, "출석 데이터를 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
                    Log.e("Attendance", "응답 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Attendance>> call, Throwable t) {
                Toast.makeText(StudentAttendanceActivity.this, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("StudentAttendance", "API 실패", t);
            }
        });
    }
}
