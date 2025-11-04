package com.mobile.greenacademypartner.ui.attendance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.model.attendance.AttendanceResponse;
import com.mobile.greenacademypartner.model.classes.Course;
import com.mobile.greenacademypartner.ui.adapter.AttendanceAdapter;
import com.mobile.greenacademypartner.ui.main.MainActivity;
import com.mobile.greenacademypartner.ui.mypage.MyPageActivity;
import com.mobile.greenacademypartner.ui.timetable.QRScannerActivity;
import com.mobile.greenacademypartner.ui.timetable.StudentTimetableActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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

    public static final String EXTRA_OVERRIDE_STUDENT_ID = "overrideStudentId";

    private Toolbar toolbar;
    private RecyclerView attendanceListView;
    private AttendanceAdapter adapter;

    private final List<AttendanceResponse> allAttendances = new ArrayList<>();
    private final Map<String, List<Integer>> classDowMap = new HashMap<>();

    private TextView tvPresent, tvLate, tvAbsent;
    private static final TimeZone KST = TimeZone.getTimeZone("Asia/Seoul");
    private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
    private int currentDisplayDow = -1;

    // ✅ 선택 날짜(기본: 오늘)
    private LocalDate selectedDate = LocalDate.now();

    // 하단 네비 & 토글
    private BottomNavigationView bottomNavigationView;
    private ImageButton btnHideNav, btnShowNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        toolbar = findViewById(R.id.toolbar);
        attendanceListView = findViewById(R.id.attendance_list_view);

        tvPresent = findViewById(R.id.tv_present_count);
        tvLate    = findViewById(R.id.tv_late_count);
        tvAbsent  = findViewById(R.id.tv_absent_count);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("출석 관리");
        ThemeColorUtil.applyThemeColor(this, toolbar);

        attendanceListView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(this, new ArrayList<>());
        attendanceListView.setAdapter(adapter);

        // 네비게이션
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnHideNav = findViewById(R.id.btn_hide_nav);
        btnShowNav = findViewById(R.id.btn_show_nav);
        bottomNavigationView.setSelectedItemId(R.id.nav_attendance);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (id == R.id.nav_attendance) {
                return true;
            } else if (id == R.id.nav_qr) {
                startActivity(new Intent(this, QRScannerActivity.class));
                return true;
            } else if (id == R.id.nav_timetable) {
                startActivity(new Intent(this, StudentTimetableActivity.class));
                return true;
            } else if (id == R.id.nav_my) {
                startActivity(new Intent(this, MyPageActivity.class));
                return true;
            }
            return false;
        });
        btnHideNav.setOnClickListener(v -> {
            bottomNavigationView.setVisibility(android.view.View.GONE);
            btnHideNav.setVisibility(android.view.View.GONE);
            btnShowNav.setVisibility(android.view.View.VISIBLE);
        });
        btnShowNav.setOnClickListener(v -> {
            bottomNavigationView.setVisibility(android.view.View.VISIBLE);
            btnHideNav.setVisibility(android.view.View.VISIBLE);
            btnShowNav.setVisibility(android.view.View.GONE);
        });

        // 데이터 로드
        fetchClassesThenAttendance();
    }

    /** classes → Days_Of_Week 맵을 만든 뒤 attendance를 불러옵니다. */
    private void fetchClassesThenAttendance() {
        String studentId = resolveTargetStudentId();
        if (studentId == null || studentId.trim().isEmpty()) {
            Toast.makeText(this, "로그인(또는 자녀 선택) 정보가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
        api.getMyClasses(studentId).enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    initTodayDowAndFetchAttendance(studentId);
                    return;
                }

                List<Course> classes = response.body();
                classDowMap.clear();
                for (Course c : classes) {
                    String key = safe(c.getClassName());
                    List<Integer> dows = c.getDaysOfWeek();
                    if (!key.isEmpty() && dows != null && !dows.isEmpty()) {
                        classDowMap.put(key, dows);
                    }
                }

                adapter.setClassDowMap(classDowMap);
                currentDisplayDow = getTodayDowMon1ToSun7();
                adapter.setDisplayDow(currentDisplayDow);
                fetchAttendanceFromServer(studentId);
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
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
                    return;
                }

                List<AttendanceResponse> list = response.body();
                try {
                    Collections.sort(list, Comparator.comparing(AttendanceResponse::getDate, String::compareTo));
                } catch (Exception ignored) {}

                allAttendances.clear();
                allAttendances.addAll(list);

                // ✅ 선택 날짜 기준으로 즉시 반영
                applyDateFilterAndBind();
            }

            @Override
            public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                Toast.makeText(StudentAttendanceActivity.this, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
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

    private void showDatePickerAndApplyLikeTimetable() {
        java.util.Calendar now = java.util.Calendar.getInstance(KST, Locale.KOREA);
        int y = now.get(java.util.Calendar.YEAR);
        int m = now.get(java.util.Calendar.MONTH);
        int d = now.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog dlg = new android.app.DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    // ✅ 선택 날짜 저장
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth);

                    // (옵션) 어댑터의 요일 하이라이트 유지
                    int c = selectedDate.getDayOfWeek().getValue(); // Mon=1..Sun=7
                    currentDisplayDow = c;
                    adapter.setDisplayDow(c);

                    // ✅ 날짜 필터 적용
                    applyDateFilterAndBind();
                },
                y, m, d
        );
        dlg.show();
    }

    private int getTodayDowMon1ToSun7() {
        java.util.Calendar cal = java.util.Calendar.getInstance(KST, Locale.KOREA);
        int c = cal.get(java.util.Calendar.DAY_OF_WEEK);
        return (c == java.util.Calendar.SUNDAY) ? 7 : (c - 1);
    }

    // ------------------------------
    // 날짜 필터링 & 요약 갱신
    // ------------------------------
    private void applyDateFilterAndBind() {
        List<AttendanceResponse> filtered = new ArrayList<>();
        long present = 0, late = 0, absent = 0;

        for (AttendanceResponse ar : allAttendances) {
            if (isSameSelectedDay(ar)) {
                filtered.add(ar);
                String norm = normalizeStatus(ar.getStatus());
                if ("출석".equals(norm)) present++;
                else if ("지각".equals(norm)) late++;
                else if ("결석".equals(norm)) absent++;
            }
        }

        // 리스트/요약 갱신
        adapter.setAll(filtered);
        tvPresent.setText("출석 " + present);
        tvLate.setText("지각 " + late);
        tvAbsent.setText("결석 " + absent);
    }

    /** AttendanceResponse의 날짜 문자열을 반환 (이 프로젝트는 getDate만 존재) */
    private String pickDateString(AttendanceResponse ar) {
        return ar != null ? ar.getDate() : null;
    }

    /** 아이템이 선택 날짜와 같은 날인지 (KST 기준 안전 비교) */
    private boolean isSameSelectedDay(AttendanceResponse ar) {
        String raw = pickDateString(ar);
        if (raw == null || raw.isEmpty()) return false;

        // 1) "yyyy-MM-dd" 형식은 앞 10자리 비교
        if (raw.length() >= 10) {
            try {
                LocalDate d = LocalDate.parse(raw.substring(0, 10));
                return d.equals(selectedDate);
            } catch (Exception ignored) {}
        }

        // 2) ISO-8601(+Z/+09:00/.SSS) → KST 변환 후 비교
        try { return Instant.parse(raw).atZone(ZONE_KST).toLocalDate().equals(selectedDate); } catch (Exception ignored) {}
        try { return OffsetDateTime.parse(raw).atZoneSameInstant(ZONE_KST).toLocalDate().equals(selectedDate); } catch (Exception ignored) {}

        // 3) 마지막 시도
        try { return LocalDate.parse(raw).equals(selectedDate); } catch (Exception ignored) {}

        return false;
    }

    // ------------------------------
    // 유틸
    // ------------------------------
    private static String normalizeStatus(String s) {
        if (s == null) return "";
        String raw = s.trim();
        String compact = raw.replaceAll("\\s+", "");
        if (compact.contains("결석")) return "결석";
        if (compact.contains("지각")) return "지각";
        if (compact.contains("출석")) return "출석";
        return raw;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private String resolveTargetStudentId() {
        Intent it = getIntent();
        if (it != null && it.hasExtra(EXTRA_OVERRIDE_STUDENT_ID)) {
            String s = it.getStringExtra(EXTRA_OVERRIDE_STUDENT_ID);
            if (s != null && !s.trim().isEmpty()) return s.trim();
            int v = it.getIntExtra(EXTRA_OVERRIDE_STUDENT_ID, -1);
            if (v != -1) return String.valueOf(v);
        }
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        return prefs.getString("username", null);
    }
}
