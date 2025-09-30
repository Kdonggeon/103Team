package com.mobile.greenacademypartner.ui.attendance;

import android.content.Intent;
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
import com.mobile.greenacademypartner.menu.ToolbarIconUtil;
import com.mobile.greenacademypartner.model.attendance.Attendance;
import com.mobile.greenacademypartner.model.attendance.AttendanceEntry;
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

    /** 학부모 → 학생 화면 재사용용 오버라이드 키 */
    public static final String EXTRA_OVERRIDE_STUDENT_ID = "overrideStudentId";

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private RecyclerView attendanceListView;
    private AttendanceAdapter adapter;

    /** 원본 전체 출석 목록(필터 전) */
    private final List<AttendanceResponse> allAttendances = new ArrayList<>();

    /** key=수업명 → Days_Of_Week(1=월 … 7=일) */
    private final Map<String, List<Integer>> classDowMap = new HashMap<>();

    private TextView tvPresent, tvLate, tvAbsent;

    private static final TimeZone KST = TimeZone.getTimeZone("Asia/Seoul");
    private int currentDisplayDow = -1; // 1=월 … 7=일

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
        adapter = new AttendanceAdapter(this, new ArrayList<>());
        attendanceListView.setAdapter(adapter);

        // 1) classes 먼저 → Days_Of_Week 맵 생성/주입
        // 2) attendance 불러와서 전체 주입
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
        ToolbarIconUtil.applyWhiteIcons(toolbar, toggle);

        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, 1);
    }

    /** classes → Days_Of_Week 맵을 만든 뒤 attendance를 불러옵니다. */
    private void fetchClassesThenAttendance() {
        String studentId = resolveTargetStudentId();
        if (studentId == null || studentId.trim().isEmpty()) {
            Toast.makeText(this, "로그인(또는 자녀 선택) 정보가 없습니다", Toast.LENGTH_SHORT).show();
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
                    initTodayDowAndFetchAttendance(studentId);
                    return;
                }

                List<Course> classes = response.body();
                classDowMap.clear();
                for (Course c : classes) {
                    String key = safe(c.getClassName()); // 현재는 수업명으로 매칭
                    List<Integer> dows = c.getDaysOfWeek(); // 1=월 … 7=일
                    if (key.isEmpty() || dows == null || dows.isEmpty()) continue;
                    classDowMap.put(key, dows);
                }

                // 어댑터에 Days_Of_Week 맵 전달
                adapter.setClassDowMap(classDowMap);

                // 초기 진입: 오늘 요일로 필터
                currentDisplayDow = getTodayDowMon1ToSun7();
                adapter.setDisplayDow(currentDisplayDow);

                // 이어서 출석 데이터 호출
                fetchAttendanceFromServer(studentId);
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                Log.e("Classes", "수업 목록 API 실패", t);
                // 그래도 출석 데이터는 불러온다
                initTodayDowAndFetchAttendance(studentId);
            }
        });
    }

    private void initTodayDowAndFetchAttendance(String studentId) {
        currentDisplayDow = getTodayDowMon1ToSun7();
        if (adapter != null) adapter.setDisplayDow(currentDisplayDow);
        fetchAttendanceFromServer(studentId);
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

                allAttendances.clear();
                allAttendances.addAll(list);

                // 원본 전체 주입(어댑터는 내부에서 현재 선택 요일 + classDowMap으로 필터)
                adapter.setAll(list);

                // 상단 요약: 현재 표시 요일 기준으로 계산
                updateSummaryCountsForDow(currentDisplayDow);
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

    // 날짜 선택 → 요일 계산 → 어댑터에 요일 전달 (Days_Of_Week만으로 필터) + 상단 요약 갱신
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
                    currentDisplayDow = dow;
                    if (adapter != null) adapter.setDisplayDow(dow);
                    updateSummaryCountsForDow(dow);
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

    /** 선택된 요일 기준으로 상단 요약(출석/지각/결석) 재계산 */
    private void updateSummaryCountsForDow(int dowMon1ToSun7) {
        if (dowMon1ToSun7 < 1 || dowMon1ToSun7 > 7) {
            // 범위 밖이면 전체 기준으로라도 표시
            updateSummaryCounts(allAttendances);
            return;
        }
        List<AttendanceResponse> filtered = new ArrayList<>();
        for (AttendanceResponse ar : allAttendances) {
            String cls = safe(ar.getClassName());
            List<Integer> dows = classDowMap.get(cls);
            if (dows != null && dows.contains(dowMon1ToSun7)) {
                filtered.add(ar);
            }
        }
        updateSummaryCounts(filtered);
    }

    /** 전달된 목록 기준으로 출석/지각/결석 카운트 후 상단에 표시 */
    private void updateSummaryCounts(List<AttendanceResponse> list) {
        long present = 0, late = 0, absent = 0;
        for (AttendanceResponse ar : list) {
            String norm = normalizeStatus(ar.getStatus());
            if ("출석".equals(norm)) present++;
            else if ("지각".equals(norm)) late++;
            else if ("결석".equals(norm)) absent++;
        }
        tvPresent.setText("출석 " + present);
        tvLate.setText("지각 " + late);
        tvAbsent.setText("결석 " + absent);
    }

    private static String normalizeStatus(String s) {
        if (s == null) return "";
        String raw = s.trim();
        String compact = raw.replaceAll("\\s+", "");
        if (compact.contains("결석")) return "결석";
        if (compact.contains("지각")) return "지각";
        if (compact.contains("출석")) return "출석";
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("absent") || lower.contains("absence")) return "결석";
        if (lower.startsWith("late") || lower.contains("tardy")) return "지각";
        if (lower.startsWith("present") || lower.contains("attend")) return "출석";
        return raw;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /**
     * 학부모가 자녀 선택 후 넘겨준 overrideStudentId(문자/정수 모두 대응)를 우선 사용하고,
     * 없으면 기존 로그인 정보(username)를 사용합니다.
     */
    private String resolveTargetStudentId() {
        Intent it = getIntent();
        if (it != null && it.hasExtra(EXTRA_OVERRIDE_STUDENT_ID)) {
            // 1) 문자열 형태 시도
            String s = it.getStringExtra(EXTRA_OVERRIDE_STUDENT_ID);
            if (s != null && !s.trim().isEmpty()) return s.trim();
            // 2) 정수 형태 시도
            int v = it.getIntExtra(EXTRA_OVERRIDE_STUDENT_ID, -1);
            if (v != -1) return String.valueOf(v);
        }
        // 3) 기존(학생 로그인) 로직 유지
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        return prefs.getString("username", null);
    }
}
