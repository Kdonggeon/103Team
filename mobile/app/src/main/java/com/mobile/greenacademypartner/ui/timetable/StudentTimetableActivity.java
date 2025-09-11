package com.mobile.greenacademypartner.ui.timetable;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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
import com.mobile.greenacademypartner.model.attendance.AttendanceResponse; // ★ 변경: Response DTO
import com.mobile.greenacademypartner.ui.adapter.AttendanceAdapter;        // ★ AttendanceResponse용 어댑터
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentTimetableActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private RecyclerView recyclerView;
    private AttendanceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_timetable);

        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("시간표");
        setSupportActionBar(toolbar);
        ThemeColorUtil.applyThemeColor(this, toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, 2);

        Button btnScanQr = findViewById(R.id.btn_scan_qr);
        btnScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(StudentTimetableActivity.this, QRScannerActivity.class);
            startActivity(intent);
        });

        recyclerView = findViewById(R.id.recycler_today_attendance);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadAttendance(); // ★ 메서드명만 의미상 변경
    }

    private void loadAttendance() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String studentId = prefs.getString("username", null);
        if (studentId == null) {
            Toast.makeText(this, "로그인 정보가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);

        // ★ 변경: Attendance → AttendanceResponse
        Call<List<AttendanceResponse>> call = api.getAttendanceForStudent(studentId);
        call.enqueue(new Callback<List<AttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AttendanceResponse> list = response.body();

                    // 🔢 날짜 오름차순(과거 → 최근) 정렬
                    Collections.sort(list, Comparator.comparing(AttendanceResponse::getDate, String::compareTo));

                    // ✅ 어댑터 연결 (item_attendance.xml 사용)
                    adapter = new AttendanceAdapter(StudentTimetableActivity.this, list);
                    recyclerView.setAdapter(adapter);

                } else {
                    Toast.makeText(StudentTimetableActivity.this, "출석 데이터를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                    Log.e("StudentTimetable", "응답 실패 code=" + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                Log.e("StudentTimetable", "API 실패", t);
                Toast.makeText(StudentTimetableActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
