package com.mobile.greenacademypartner.ui.attendance;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

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
import java.util.Comparator;
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
    private String academyName = "";   // 🔥 학원 이름 저장

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        toolbar = findViewById(R.id.toolbar);
        listView = findViewById(R.id.attendance_list_view);

        tvPresent = findViewById(R.id.tv_present_count);
        tvLate = findViewById(R.id.tv_late_count);
        tvAbsent = findViewById(R.id.tv_absent_count);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("출석 관리");
        ThemeColorUtil.applyThemeColor(this, toolbar);

        listView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(this, new ArrayList<>());
        listView.setAdapter(adapter);

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_attendance);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.nav_qr) startActivity(new Intent(this, QRScannerActivity.class));
            else if (id == R.id.nav_timetable)
                startActivity(new Intent(this, StudentTimetableActivity.class));
            else if (id == R.id.nav_my) startActivity(new Intent(this, MyPageActivity.class));
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

        loadPrefs();
        loadTodayClassesAndAttendance();
    }

    // 🔥 sharedPreferences 에서 학원 이름 / 학생 ID 가져오기
    private void loadPrefs() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);

        studentId = prefs.getString("username", null);
        if (studentId == null) {
            Toast.makeText(this, "학생 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
        }

        int academyNum = prefs.getInt("academyNumber", -1);
        if (academyNum != -1) {
            academyName = academyNum + "학원";
        } else {
            academyName = "";
        }
    }

    //────────────────────────────────────────────
    //  달력 메뉴
    //────────────────────────────────────────────
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_student_timetable, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_pick_date) {
            openDatePicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openDatePicker() {
        LocalDate now = selectedDate;

        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    loadTodayClassesAndAttendance();
                },
                now.getYear(),
                now.getMonthValue() - 1,
                now.getDayOfMonth()
        );
        dlg.show();
    }

    //────────────────────────────────────────────
    //  수업 조회
    //────────────────────────────────────────────
    private void loadTodayClassesAndAttendance() {

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);

        api.getMyClasses(studentId).enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(StudentAttendanceActivity.this, "수업 조회 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                int dow = selectedDate.getDayOfWeek().getValue();
                todayClasses.clear();

                for (Course c : response.body()) {
                    if (c.getDaysOfWeek() != null && c.getDaysOfWeek().contains(dow)) {
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

    //────────────────────────────────────────────
    //  출석 조회
    //────────────────────────────────────────────
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
                for (AttendanceResponse ar : response.body()) {
                    if (ar.getDate() != null && ar.getDate().startsWith(selectedDate.toString())) {
                        todayAttend.add(ar);
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

    //────────────────────────────────────────────
    //  미래=예정 / 오늘=예정·수업중·결석 / 과거=결석
    //────────────────────────────────────────────
    private void mergeClassAndAttendance() {
        List<AttendanceResponse> finalList = new ArrayList<>();

        long present = 0, late = 0, absent = 0;

        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        for (Course c : todayClasses) {

            AttendanceResponse matched = null;

            for (AttendanceResponse ar : todayAttend) {
                if (ar.getClassName().equals(c.getClassName())) {
                    matched = ar;
                    break;
                }
            }

            // 🔥 미래 날짜
            if (selectedDate.isAfter(today)) {
                AttendanceResponse future = new AttendanceResponse();
                future.setClassName(c.getClassName());
                future.setAcademyName(academyName);
                future.setDate(selectedDate.toString());
                future.setStatus("예정");
                future.setStartTime(c.getStartTime());
                future.setEndTime(c.getEndTime());
                finalList.add(future);
                continue;
            }

            // 🔥 과거 날짜
            if (selectedDate.isBefore(today)) {
                if (matched == null) {
                    AttendanceResponse ab = new AttendanceResponse();
                    ab.setClassName(c.getClassName());
                    ab.setAcademyName(academyName);
                    ab.setDate(selectedDate.toString());
                    ab.setStatus("결석");
                    ab.setStartTime(c.getStartTime());
                    ab.setEndTime(c.getEndTime());
                    finalList.add(ab);
                    absent++;
                } else {
                    finalList.add(matched);
                    String s = matched.getStatus();
                    if (s.contains("출석")) present++;
                    else if (s.contains("지각")) late++;
                    else absent++;
                }
                continue;
            }

            // 🔥 오늘 날짜
            LocalTime classStart = LocalTime.parse(c.getStartTime());
            LocalTime classEnd = LocalTime.parse(c.getEndTime());

            if (matched != null) {
                if (matched.getAcademyName() == null || matched.getAcademyName().isEmpty()) {
                    matched.setAcademyName(academyName);
                }

                finalList.add(matched);
                String s = matched.getStatus();

                if (s.contains("출석")) present++;
                else if (s.contains("지각")) late++;
                else absent++;

            } else {
                AttendanceResponse ab = new AttendanceResponse();
                ab.setClassName(c.getClassName());
                ab.setAcademyName(academyName);
                ab.setDate(selectedDate.toString());
                ab.setStartTime(c.getStartTime());
                ab.setEndTime(c.getEndTime());

                if (nowTime.isBefore(classStart)) {
                    ab.setStatus("예정");      // 🔥 수업 시작 전
                } else if (nowTime.isAfter(classEnd)) {
                    ab.setStatus("결석");      // 🔥 수업 끝났는데 출석 없음
                    absent++;
                } else {
                    ab.setStatus("수업중");    // 🔥 수업 중
                }

                finalList.add(ab);
            }
        }

        finalList.sort(Comparator.comparing(AttendanceResponse::getStartTime));
        adapter.setAll(finalList);

        // 🔥 미래는 요약 0으로
        if (selectedDate.isAfter(today)) {
            tvPresent.setText("출석 0");
            tvLate.setText("지각 0");
            tvAbsent.setText("결석 0");
            return;
        }

        tvPresent.setText("출석 " + present);
        tvLate.setText("지각 " + late);
        tvAbsent.setText("결석 " + absent);
    }
}
