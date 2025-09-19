package com.mobile.greenacademypartner.ui.attendance;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.mobile.greenacademypartner.model.classes.Course;
import com.mobile.greenacademypartner.ui.adapter.AttendanceAdapter;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private RecyclerView attendanceListView;
    private AttendanceAdapter adapter;

    private final List<AttendanceResponse> allAttendances = new ArrayList<>(); // 원본 유지

    private final Map<String, List<Integer>> classDowMap = new HashMap<>();    // key=수업명 → Days_Of_Week
    private TextView tvPresent, tvLate, tvAbsent;

    private static final TimeZone KST = TimeZone.getTimeZone("Asia/Seoul");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        attendanceListView = findViewById(R.id.attendance_list_view);

        tvPresent = findViewById(R.id.tv_present_count);
        tvLate    = findViewById(R.id.tv_late_count);
        tvAbsent  = findViewById(R.id.tv_absent_count);

        int white = androidx.core.content.ContextCompat.getColor(this, android.R.color.white);
        toolbar.setTitleTextColor(white);
        if (toolbar.getNavigationIcon() != null) toolbar.getNavigationIcon().setTint(white);
        if (toolbar.getOverflowIcon() != null) toolbar.getOverflowIcon().setTint(white);

        setupToolbarAndDrawer();

        attendanceListView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(this, new ArrayList<>()); // 시간표 방식 어댑터
        attendanceListView.setAdapter(adapter);

        // 1) classes 먼저 로드 → Days_Of_Week 맵 생성 → 어댑터에 주입
        // 2) attendance 로드 → 어댑터에 전체 주입
        fetchClassesThenAttendance();
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

    /** classes → Days_Of_Week 맵을 만든 뒤 attendance를 불러옵니다. */
    private void fetchClassesThenAttendance() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String studentId = prefs.getString("username", null);
        if (studentId == null) {
            Toast.makeText(this, "로그인 정보가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);

        // 시간표 화면과 동일: 학생의 수업 목록 호출 (getMyClasses)
        api.getMyClasses(studentId).enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("Classes", "수업 목록 응답 실패: " + response.code());
                    // 그래도 출석 데이터는 불러온다 (맵 비어있으면 필터 결과가 비게 됨)
                    fetchAttendanceFromServer(studentId);
                    return;
                }

                List<Course> classes = response.body();
                classDowMap.clear();
                for (Course c : classes) {
                    String key = safe(c.getClassName()); // ★ 현재는 수업명으로 매칭(시간표와 동일)
                    List<Integer> dows = c.getDaysOfWeek(); // 1=월 … 7=일
                    if (key.isEmpty() || dows == null || dows.isEmpty()) continue;
                    classDowMap.put(key, dows);
                }

                // 어댑터에 Days_Of_Week 맵 전달
                adapter.setClassDowMap(classDowMap);

                // 초기 진입: 오늘 요일로 필터
                adapter.setDisplayDow(getTodayDowMon1ToSun7());

                // 이어서 출석 데이터 호출
                fetchAttendanceFromServer(studentId);
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                Log.e("Classes", "수업 목록 API 실패", t);
                // 그래도 출석 데이터는 불러온다
                fetchAttendanceFromServer(studentId);
            }
        });
    }

    private void fetchAttendanceFromServer(String studentId) {
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
                Collections.sort(list, Comparator.comparing(AttendanceResponse::getDate, String::compareTo));

                long present = 0, late = 0, absent = 0;
                for (AttendanceResponse ar : list) {
                    String norm = normalizeStatus(ar.getStatus());
                    if ("출석".equals(norm)) present++;
                    else if ("지각".equals(norm)) late++;
                    else if ("결석".equals(norm)) absent++;
                }
                for (AttendanceResponse ar : list) {
                    Log.d("ATT-CNT", "[" + safe(ar.getClassName()) + " " + safe(ar.getDate()) + "] raw='" + ar.getStatus() + "' -> norm='" + normalizeStatus(ar.getStatus()) + "'");
                }

                tvPresent.setText("출석 " + present);
                tvLate.setText("지각 " + late);
                tvAbsent.setText("결석 " + absent);

                allAttendances.clear();
                allAttendances.addAll(list);

                // 원본 전체 주입(어댑터는 내부에서 현재 선택 요일 + classDowMap으로 필터)
                adapter.setAll(list);
            }

            @Override
            public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                Toast.makeText(StudentAttendanceActivity.this, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("StudentAttendance", "API 실패", t);
            }
        });

        ThemeColorUtil.applyThemeColor(this, toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        // 시간표의 메뉴 XML 재사용 (action_pick_date 포함)
        getMenuInflater().inflate(R.menu.menu_student_timetable, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_pick_date) {
            showDatePickerAndApplyLikeTimetable();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 날짜 선택 → 요일 계산 → 어댑터에 요일 전달 (Days_Of_Week만으로 필터)
    private void showDatePickerAndApplyLikeTimetable() {
        java.util.Calendar now = java.util.Calendar.getInstance(KST, Locale.KOREA);
        int y = now.get(java.util.Calendar.YEAR);
        int m = now.get(java.util.Calendar.MONTH);
        int d = now.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog dlg = new android.app.DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    java.util.Calendar cal = java.util.Calendar.getInstance(KST, Locale.KOREA);
                    cal.setLenient(false);
                    cal.set(java.util.Calendar.YEAR, year);
                    cal.set(java.util.Calendar.MONTH, month); // 0-based
                    cal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
                    int c = cal.get(java.util.Calendar.DAY_OF_WEEK); // SUNDAY=1 … SATURDAY=7
                    int dow = (c == java.util.Calendar.SUNDAY) ? 7 : (c - 1); // 1=월 … 7=일
                    if (adapter != null) adapter.setDisplayDow(dow);
                },
                y, m, d
        );
        dlg.show();
    }

    private int getTodayDowMon1ToSun7() {
        java.util.Calendar cal = java.util.Calendar.getInstance(KST, Locale.KOREA);
        int c = cal.get(java.util.Calendar.DAY_OF_WEEK); // SUNDAY=1 … SATURDAY=7
        return (c == java.util.Calendar.SUNDAY) ? 7 : (c - 1);
    }

    private static String normalizeStatus(String s) {
        if (s == null) return "";
        String raw = s.trim();
        String compact = raw.replaceAll("\\s+", "");
        if (compact.contains("결석")) return "결석";
        if (compact.contains("지각")) return "지각";
        if (compact.contains("출석")) return "출석";
        String lower = raw.toLowerCase();
        if (lower.startsWith("absent") || lower.contains("absence")) return "결석";
        if (lower.startsWith("late") || lower.contains("tardy")) return "지각";
        if (lower.startsWith("present") || lower.contains("attend")) return "출석";
        return raw;
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
