package com.mobile.greenacademypartner.ui.attendance;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView; // ★ 추가
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
import com.mobile.greenacademypartner.model.attendance.AttendanceResponse; // ★ AttendanceResponse 사용
import com.mobile.greenacademypartner.ui.adapter.AttendanceAdapter;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private RecyclerView attendanceListView;

    // ★ 요약 박스 TextView
    private TextView tvPresent, tvLate, tvAbsent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        //  뷰 초기화
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        attendanceListView = findViewById(R.id.attendance_list_view);

        // ★ 요약 박스 연결
        tvPresent = findViewById(R.id.tv_present_count);
        tvLate    = findViewById(R.id.tv_late_count);
        tvAbsent  = findViewById(R.id.tv_absent_count);

        int white = androidx.core.content.ContextCompat.getColor(this, android.R.color.white);
        toolbar.setTitleTextColor(white);
        if (toolbar.getNavigationIcon() != null) toolbar.getNavigationIcon().setTint(white);
        if (toolbar.getOverflowIcon() != null) toolbar.getOverflowIcon().setTint(white);

        setupToolbarAndDrawer(); // 🛠️ 사이드바 및 툴바 설정
        fetchAttendanceFromServer(); // 📡 출석 데이터 로드
    }

    private void setupToolbarAndDrawer() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("출석 관리");
        ToolbarColorUtil.applyToolbarColor(this, toolbar);

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
        api.getAttendanceForStudent(studentId).enqueue(new Callback<List<AttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(StudentAttendanceActivity.this, "출석 데이터를 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
                    Log.e("Attendance", "응답 실패: " + response.code());
                    return;
                }

                List<AttendanceResponse> list = response.body();

                //  날짜 오름차순(과거 → 최근)
                Collections.sort(list, Comparator.comparing(AttendanceResponse::getDate, String::compareTo));

                //  합계 계산 (상태 문자열 정규화)
                long present = 0, late = 0, absent = 0;
                for (AttendanceResponse ar : list) {
                    String norm = normalizeStatus(ar.getStatus());
                    if ("출석".equals(norm)) present++;
                    else if ("지각".equals(norm)) late++;
                    else if ("결석".equals(norm)) absent++;
                }

                //  디버깅 로그 (원상태 → 정규화 상태)
                for (AttendanceResponse ar : list) {
                    Log.d("ATT-CNT", "[" + ar.getClassName() + " " + ar.getDate() + "] raw='" + ar.getStatus() + "' -> norm='" + normalizeStatus(ar.getStatus()) + "'");
                }

                //  합계 반영
                tvPresent.setText("출석 " + present);
                tvLate.setText("지각 " + late);
                tvAbsent.setText("결석 " + absent);

                //  리스트 표시
                attendanceListView.setLayoutManager(new LinearLayoutManager(StudentAttendanceActivity.this));
                AttendanceAdapter adapter = new AttendanceAdapter(StudentAttendanceActivity.this, list);
                attendanceListView.setAdapter(adapter);

                // 로그
                for (AttendanceResponse att : list) {
                    Log.d("Attendance", "학원명=" + att.getAcademyName()
                            + ", 수업명=" + att.getClassName()
                            + ", 날짜=" + att.getDate()
                            + ", 상태=" + att.getStatus());
                }
            }

            @Override
            public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                Toast.makeText(StudentAttendanceActivity.this, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("StudentAttendance", "API 실패", t);
            }
        });

        ThemeColorUtil.applyThemeColor(this, toolbar);
    }

    /** 상태 문자열을 표준화: 공백/괄호/대소문자/영문 대응 */
    private static String normalizeStatus(String s) {
        if (s == null) return "";
        String raw = s.trim();
        String compact = raw.replaceAll("\\s+", "");
        // 한글 키워드 부분일치
        if (compact.contains("결석")) return "결석";
        if (compact.contains("지각")) return "지각";
        if (compact.contains("출석")) return "출석";
        // 영문 키워드
        String lower = raw.toLowerCase();
        if (lower.startsWith("absent") || lower.contains("absence")) return "결석";
        if (lower.startsWith("late") || lower.contains("tardy")) return "지각";
        if (lower.startsWith("present") || lower.contains("attend")) return "출석";
        return raw; // 모르는 값은 원문 유지(로그로 확인)
    }
}
