package com.mobile.greenacademypartner.ui.attendance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.model.attendance.AttendanceResponse;
import com.mobile.greenacademypartner.model.classes.Course;
import com.mobile.greenacademypartner.model.student.Student;
import com.mobile.greenacademypartner.ui.adapter.AttendanceAdapter;
import com.mobile.greenacademypartner.ui.main.MainActivity;
import com.mobile.greenacademypartner.ui.mypage.MyPageActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;
import com.mobile.greenacademypartner.ui.timetable.QRScannerActivity;
import com.mobile.greenacademypartner.ui.timetable.StudentTimetableActivity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParentAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView attendanceListView;
    private Spinner spinnerChildren;

    private TextView tvPresent, tvLate, tvAbsent;

    private SharedPreferences prefs;
    private StudentApi studentApi;
    private ParentApi parentApi;

    private AttendanceAdapter adapter;

    private final List<Course> todayClasses = new ArrayList<>();
    private final List<AttendanceResponse> todayAttend = new ArrayList<>();

    private LocalDate selectedDate = LocalDate.now(); // 🔥 선택 날짜
    private List<Student> childList = new ArrayList<>();
    private Student selectedChild = null;

    private ImageButton btnHideNav, btnShowNav;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_attendance);

        toolbar = findViewById(R.id.toolbar);
        attendanceListView = findViewById(R.id.attendance_list_view);
        spinnerChildren = findViewById(R.id.spinner_children);

        tvPresent = findViewById(R.id.tv_present_count);
        tvLate = findViewById(R.id.tv_late_count);
        tvAbsent = findViewById(R.id.tv_absent_count);

        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));
        setSupportActionBar(toolbar);
        setTitle("자녀 출석 확인");
        ThemeColorUtil.applyThemeColor(this, toolbar);

        prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        studentApi = RetrofitClient.getClient().create(StudentApi.class);
        parentApi = RetrofitClient.getClient().create(ParentApi.class);

        attendanceListView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(this, new ArrayList<>());
        attendanceListView.setAdapter(adapter);

        String parentId = prefs.getString("userId", null);
        if (parentId == null || parentId.isEmpty())
            parentId = prefs.getString("parentId", null);
        if (parentId == null || parentId.isEmpty())
            parentId = prefs.getString("username", null);

        if (parentId == null || parentId.isEmpty()) {
            Toast.makeText(this, "학부모 로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchChildren(parentId);
        setupBottomNav();
    }

    private void setupBottomNav() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnHideNav = findViewById(R.id.btn_hide_nav);
        btnShowNav = findViewById(R.id.btn_show_nav);

        bottomNavigationView.setSelectedItemId(R.id.nav_attendance);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.nav_qr) startActivity(new Intent(this, QRScannerActivity.class));
            else if (id == R.id.nav_timetable) startActivity(new Intent(this, StudentTimetableActivity.class));
            else if (id == R.id.nav_my) startActivity(new Intent(this, MyPageActivity.class));
            return true;
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
    }

    /** 🔥 학생 목록 불러오기 */
    private void fetchChildren(String parentId) {
        parentApi.getChildrenByParentId(parentId).enqueue(new Callback<List<Student>>() {
            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    spinnerChildren.setAdapter(new ArrayAdapter<>(ParentAttendanceActivity.this,
                            android.R.layout.simple_spinner_item,
                            new String[]{"자녀 없음"}));
                    return;
                }

                childList = response.body();
                List<String> names = new ArrayList<>();
                for (Student s : childList) names.add(s.getStudentName());

                ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(
                        ParentAttendanceActivity.this,
                        android.R.layout.simple_spinner_item,
                        names
                );
                spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerChildren.setAdapter(spinAdapter);

                spinnerChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                        selectedChild = childList.get(pos);
                        loadStudentAttendance(selectedChild.getStudentId());
                    }

                    @Override public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            @Override
            public void onFailure(Call<List<Student>> call, Throwable t) {
                spinnerChildren.setAdapter(new ArrayAdapter<>(ParentAttendanceActivity.this,
                        android.R.layout.simple_spinner_item,
                        new String[]{"자녀 없음"}));
            }
        });
    }

    private void loadStudentAttendance(String studentId) {
        todayClasses.clear();
        todayAttend.clear();

        studentApi.getMyClasses(studentId).enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ParentAttendanceActivity.this, "수업 조회 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                todayClasses.addAll(response.body());
                loadAttendance(studentId);
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                Toast.makeText(ParentAttendanceActivity.this, "수업 조회 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAttendance(String studentId) {
        studentApi.getAttendanceForStudent(studentId).enqueue(new Callback<List<AttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ParentAttendanceActivity.this, "출석 조회 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                todayAttend.addAll(response.body());
                mergeAttendance();
            }

            @Override
            public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                Toast.makeText(ParentAttendanceActivity.this, "출석 조회 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 🔥 미래는 “예정” / 오늘·과거는 출석/지각/결석 처리 */
    private void mergeAttendance() {
        List<AttendanceResponse> finalList = new ArrayList<>();

        long present = 0, late = 0, absent = 0;

        LocalDate today = LocalDate.now();
        java.time.LocalTime nowTime = java.time.LocalTime.now();

        for (Course c : todayClasses) {

            AttendanceResponse matched = null;
            for (AttendanceResponse ar : todayAttend) {
                if (ar.getClassName().equals(c.getClassName())
                        && ar.getDate().startsWith(selectedDate.toString())) {
                    matched = ar;
                    break;
                }
            }

            // 🔥 학생이 다니는 학원 표시
            String academyName = "";
            if (selectedChild != null &&
                    selectedChild.getAcademyNumbers() != null &&
                    !selectedChild.getAcademyNumbers().isEmpty()) {
                academyName = selectedChild.getAcademyNumbers().get(0) + "학원";
            }

            // ---------- 🔥 미래 날짜는 무조건 "예정" ----------
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

            // ---------- 🔥 과거 날짜 ----------
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

            // ---------- 🔥 오늘 날짜인 경우 ----------
            java.time.LocalTime classStart = java.time.LocalTime.parse(c.getStartTime());
            java.time.LocalTime classEnd   = java.time.LocalTime.parse(c.getEndTime());

            if (matched != null) {
                // 출석기록 있으면 그대로 반영
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
                    ab.setStatus("예정");

                } else if (nowTime.isAfter(classEnd)) {
                    ab.setStatus("결석");
                    absent++;

                } else {
                    // 🔥 수업 진행중
                    ab.setStatus("진행중");
                }

                finalList.add(ab);
            }

        }

        finalList.sort(Comparator.comparing(AttendanceResponse::getStartTime));
        adapter.setAll(finalList);

        // 🔥 미래 날짜 요약
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


    // 🔥 캘린더 메뉴 추가
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_student_timetable, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_pick_date) {
            openDatePicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openDatePicker() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        int y = now.get(java.util.Calendar.YEAR);
        int m = now.get(java.util.Calendar.MONTH);
        int d = now.get(java.util.Calendar.DAY_OF_MONTH);

        new android.app.DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    mergeAttendance(); // 날짜 적용
                },
                y, m, d
        ).show();
    }
}
