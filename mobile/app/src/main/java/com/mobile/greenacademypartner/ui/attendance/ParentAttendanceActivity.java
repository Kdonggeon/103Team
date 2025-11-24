package com.mobile.greenacademypartner.ui.attendance;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import java.time.LocalTime;
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

    private AttendanceAdapter adapter;

    private SharedPreferences prefs;
    private StudentApi studentApi;
    private ParentApi parentApi;

    private final List<Course> todayClasses = new ArrayList<>();
    private final List<AttendanceResponse> todayAttend = new ArrayList<>();

    private LocalDate selectedDate = LocalDate.now();

    private List<Student> childList = new ArrayList<>();
    private Student selectedChild;

    private BottomNavigationView bottomNav;
    private ImageButton btnHideNav, btnShowNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_attendance);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("출석 관리");

        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));
        ThemeColorUtil.applyThemeColor(this, toolbar);

        attendanceListView = findViewById(R.id.attendance_list_view);
        attendanceListView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(this, new ArrayList<>());
        attendanceListView.setAdapter(adapter);

        spinnerChildren = findViewById(R.id.spinner_children);

        tvPresent = findViewById(R.id.tv_present_count);
        tvLate = findViewById(R.id.tv_late_count);
        tvAbsent = findViewById(R.id.tv_absent_count);

        prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        studentApi = RetrofitClient.getClient().create(StudentApi.class);
        parentApi = RetrofitClient.getClient().create(ParentApi.class);

        setupBottomNav();

        String parentId = prefs.getString("username",
                prefs.getString("parentId",
                        prefs.getString("userId", null)));

        if (parentId == null) {
            Toast.makeText(this, "학부모 로그인 정보 없음", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchChildren(parentId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_student_timetable, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_pick_date) {

            LocalDate now = selectedDate;

            DatePickerDialog dlg = new DatePickerDialog(
                    this,
                    (v, year, month, day) -> {
                        selectedDate = LocalDate.of(year, month + 1, day);
                        if (selectedChild != null)
                            loadClasses(selectedChild.getStudentId());
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

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottom_navigation);
        btnHideNav = findViewById(R.id.btn_hide_nav);
        btnShowNav = findViewById(R.id.btn_show_nav);

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
                        loadClasses(selectedChild.getStudentId());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            @Override public void onFailure(Call<List<Student>> call, Throwable t) {}
        });
    }

    // ----------------------------------------------------
    // 수업 조회
    // ----------------------------------------------------
    private void loadClasses(String studentId) {
        todayClasses.clear();
        todayAttend.clear();

        studentApi.getMyClasses(studentId).enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ParentAttendanceActivity.this, "수업 조회 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                int dow = selectedDate.getDayOfWeek().getValue();
                todayClasses.clear();

                for (Course c : response.body()) {
                    if (c.getDaysOfWeek() != null && c.getDaysOfWeek().contains(dow))
                        todayClasses.add(c);
                }

                loadTodayAttendance(studentId);
            }

            @Override public void onFailure(Call<List<Course>> call, Throwable t) {}
        });
    }

    // ----------------------------------------------------
    // 출석 조회
    // ----------------------------------------------------
    private void loadTodayAttendance(String studentId) {

        studentApi.getAttendanceForStudent(studentId).enqueue(new Callback<List<AttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) return;

                todayAttend.clear();
                todayAttend.addAll(response.body());

                // 학원 이름 prefs에 저장
                for (AttendanceResponse ar : todayAttend) {
                    if (ar.getAcademyName() != null && !ar.getAcademyName().isEmpty()) {
                        prefs.edit().putString("academyName", ar.getAcademyName()).apply();
                        break;
                    }
                }

                mergeAttendance();
            }

            @Override public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {}
        });
    }

    // ----------------------------------------------------
    // 병합
    // ----------------------------------------------------
    private void mergeAttendance() {

        List<AttendanceResponse> finalList = new ArrayList<>();

        long present = 0, late = 0, absent = 0;

        String academyName = null;

        for (AttendanceResponse ar : todayAttend) {
            if (ar.getAcademyName() != null && !ar.getAcademyName().isEmpty()) {
                academyName = ar.getAcademyName();
                break;
            }
        }

        if (academyName == null)
            academyName = prefs.getString("academyName", "");

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

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
                item.setClassName(c.getClassName());
                item.setStartTime(c.getStartTime());
                item.setEndTime(c.getEndTime());
                item.setDate(selectedDate.toString());

                LocalTime start = LocalTime.parse(c.getStartTime());
                LocalTime end = LocalTime.parse(c.getEndTime());

                if (selectedDate.isAfter(today)) {
                    item.setStatus("예정");
                } else if (selectedDate.isBefore(today)) {
                    item.setStatus("결석");
                } else {
                    if (now.isBefore(start)) item.setStatus("예정");
                    else if (now.isAfter(end)) item.setStatus("결석");
                    else item.setStatus("진행중");
                }
            }

            item.setAcademyName(academyName);
            finalList.add(item);

            if ("출석".equals(item.getStatus())) present++;
            else if ("지각".equals(item.getStatus())) late++;
            else if ("결석".equals(item.getStatus())) absent++;
        }

        finalList.sort(Comparator.comparing(AttendanceResponse::getStartTime, Comparator.nullsLast(String::compareTo)));

        adapter.setAll(finalList);

        tvPresent.setText("출석 " + present);
        tvLate.setText("지각 " + late);
        tvAbsent.setText("결석 " + absent);
    }
}
