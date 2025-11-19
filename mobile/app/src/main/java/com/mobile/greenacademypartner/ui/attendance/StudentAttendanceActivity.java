package com.mobile.greenacademypartner.ui.attendance;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;
import com.mobile.greenacademypartner.ui.timetable.QRScannerActivity;
import com.mobile.greenacademypartner.ui.timetable.StudentTimetableActivity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView listView;
    private AttendanceAdapter adapter;

    private final List<Course> todayClasses = new ArrayList<>();
    private final List<AttendanceResponse> todayAttend = new ArrayList<>();

    private TextView tvPresent, tvLate, tvAbsent;
    private LocalDate selectedDate = LocalDate.now();

    private BottomNavigationView bottomNav;
    private ImageButton btnHideNav, btnShowNav;

    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("출석 관리");

        ThemeColorUtil.applyThemeColor(this, toolbar);

        listView = findViewById(R.id.attendance_list_view);
        listView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(this, new ArrayList<>());
        listView.setAdapter(adapter);

        tvPresent = findViewById(R.id.tv_present_count);
        tvLate = findViewById(R.id.tv_late_count);
        tvAbsent = findViewById(R.id.tv_absent_count);

        setupBottomNavigation();
        loadPrefs();
        loadTodayClassesAndAttendance();
    }

    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_attendance);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)
                startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.nav_qr)
                startActivity(new Intent(this, QRScannerActivity.class));
            else if (id == R.id.nav_timetable)
                startActivity(new Intent(this, StudentTimetableActivity.class));
            else if (id == R.id.nav_my)
                startActivity(new Intent(this, MyPageActivity.class));
            return true;
        });

        btnHideNav = findViewById(R.id.btn_hide_nav);
        btnShowNav = findViewById(R.id.btn_show_nav);

        btnHideNav.setOnClickListener(v -> {
            bottomNav.setVisibility(android.view.View.GONE);
            btnHideNav.setVisibility(android.view.View.GONE);
            btnShowNav.setVisibility(android.view.View.VISIBLE);
        });

        btnShowNav.setOnClickListener(v -> {
            bottomNav.setVisibility(android.view.View.VISIBLE);
            btnShowNav.setVisibility(android.view.View.GONE);
            btnHideNav.setVisibility(android.view.View.VISIBLE);
        });
    }

    private void loadPrefs() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        studentId = prefs.getString("username", null);
    }

    // ────────────────────────────────────────────
    // 캘린더 메뉴 추가
    // ────────────────────────────────────────────
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_student_timetable, menu);
        return true;
    }

    // 캘린더 클릭 시
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_pick_date) {

            LocalDate now = selectedDate;

            DatePickerDialog dlg = new DatePickerDialog(
                    this,
                    (v, year, month, day) -> {
                        selectedDate = LocalDate.of(year, month + 1, day);
                        loadTodayClassesAndAttendance();  // 🔥 날짜 바뀌면 즉시 갱신
                    },
                    now.getYear(),
                    now.getMonthValue() - 1,
                    now.getDayOfMonth()
            );

            dlg.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ────────────────────────────────────────────
    // 수업/출석 불러오기
    // ────────────────────────────────────────────
    private void loadTodayClassesAndAttendance() {
        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);

        api.getMyClasses(studentId).enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(StudentAttendanceActivity.this, "수업 조회 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                todayClasses.clear();

                int dow = selectedDate.getDayOfWeek().getValue(); // 월=1~일=7

                for (Course c : response.body()) {
                    if (c.getDaysOfWeek() != null &&
                            c.getDaysOfWeek().contains(dow)) {

                        todayClasses.add(c);
                    }
                }

                loadTodayAttendance();
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                Toast.makeText(StudentAttendanceActivity.this, "수업 조회 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTodayAttendance() {
        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);

        api.getAttendanceForStudent(studentId).enqueue(new Callback<List<AttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(StudentAttendanceActivity.this, "출석 조회 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                todayAttend.clear();
                todayAttend.addAll(response.body());

                // 🔥 학원 이름 prefs에 저장
                for (AttendanceResponse ar : todayAttend) {
                    if (ar.getAcademyName() != null && !ar.getAcademyName().isEmpty()) {
                        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
                        prefs.edit().putString("academyName", ar.getAcademyName()).apply();
                        break;
                    }
                }

                mergeClassAndAttendance();
            }

            @Override
            public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                Toast.makeText(StudentAttendanceActivity.this, "출석 조회 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // ────────────────────────────────────────────
    // 병합 (서버 academyName 그대로 사용)
    // ────────────────────────────────────────────
    private void mergeClassAndAttendance() {

        List<AttendanceResponse> finalList = new ArrayList<>();

        long present = 0, late = 0, absent = 0;

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 🔥 서버에서 첫 번째에 academyName이 있으면 전체 공통으로 사용
        String academyNameFromServer = null;
        for (AttendanceResponse ar : todayAttend) {
            if (ar.getAcademyName() != null && !ar.getAcademyName().isEmpty()) {
                academyNameFromServer = ar.getAcademyName();
                break;
            }
        }

        for (Course c : todayClasses) {

            AttendanceResponse matched = null;

            for (AttendanceResponse ar : todayAttend) {
                if (c.getClassName().equals(ar.getClassName())) {
                    matched = ar;
                    break;
                }
            }

            AttendanceResponse item;

            if (matched != null) {
                item = matched;
            } else {
                item = new AttendanceResponse();

                // 🔥 academyName 강제코딩 없음 → 서버 것 그대로 사용
                item.setAcademyName(academyNameFromServer);

                item.setClassName(c.getClassName());
                item.setStartTime(c.getStartTime());
                item.setEndTime(c.getEndTime());
                item.setDate(selectedDate.toString());

                if (selectedDate.isAfter(today)) {
                    item.setStatus("예정");
                } else if (selectedDate.isBefore(today)) {
                    item.setStatus("결석");
                    absent++;
                } else {
                    LocalTime start = LocalTime.parse(c.getStartTime());
                    LocalTime end = LocalTime.parse(c.getEndTime());

                    if (now.isBefore(start)) item.setStatus("예정");
                    else if (now.isAfter(end)) {
                        item.setStatus("결석");
                        absent++;
                    } else {
                        item.setStatus("수업중");
                    }
                }
            }

            if ("출석".equals(item.getStatus())) present++;
            else if ("지각".equals(item.getStatus())) late++;

            finalList.add(item);
        }

        // 정렬
        finalList.sort((a, b) -> {
            if (a.getStartTime() == null && b.getStartTime() == null) return 0;
            if (a.getStartTime() == null) return 1;
            if (b.getStartTime() == null) return -1;
            return a.getStartTime().compareTo(b.getStartTime());
        });

        adapter.setAll(finalList);

        tvPresent.setText("출석 " + present);
        tvLate.setText("지각 " + late);
        tvAbsent.setText("결석 " + absent);
    }
}
