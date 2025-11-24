package com.mobile.greenacademypartner.ui.timetable;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.model.classes.Course;
import com.mobile.greenacademypartner.model.student.Student;
import com.mobile.greenacademypartner.ui.adapter.TimetableAdapter;
import com.mobile.greenacademypartner.ui.attendance.AttendanceActivity;
import com.mobile.greenacademypartner.ui.main.MainActivity;
import com.mobile.greenacademypartner.ui.mypage.MyPageActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentTimetableActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private TimetableAdapter adapter;

    private BottomNavigationView bottomNavigation;
    private ImageButton btnHideNav, btnShowNav;

    private Spinner spinnerChildren;
    private ArrayAdapter<String> childrenAdapter;
    private final List<Student> children = new ArrayList<>();

    private String currentStudentId = null;

    private LocalDate selectedDate = LocalDate.now();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_timetable);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("시간표");
        setSupportActionBar(toolbar);
        ThemeColorUtil.applyThemeColor(this, toolbar);

        spinnerChildren = findViewById(R.id.spinner_children);
        recyclerView = findViewById(R.id.recycler_today_attendance);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        bottomNavigation = findViewById(R.id.bottom_navigation);
        btnHideNav = findViewById(R.id.btn_hide_nav);
        btnShowNav = findViewById(R.id.btn_show_nav);

        bottomNavigation.setSelectedItemId(R.id.nav_timetable);

        btnHideNav.setOnClickListener(v -> {
            bottomNavigation.setVisibility(View.GONE);
            btnHideNav.setVisibility(View.GONE);
            btnShowNav.setVisibility(View.VISIBLE);
        });

        btnShowNav.setOnClickListener(v -> {
            bottomNavigation.setVisibility(View.VISIBLE);
            btnShowNav.setVisibility(View.GONE);
            btnHideNav.setVisibility(View.VISIBLE);
        });

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)
                startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.nav_attendance)
                startActivity(new Intent(this, AttendanceActivity.class));
            else if (id == R.id.nav_qr)
                startActivity(new Intent(this, QRScannerActivity.class));
            else if (id == R.id.nav_my)
                startActivity(new Intent(this, MyPageActivity.class));
            return true;
        });

        setupStudentOrChildrenFlow();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_student_timetable, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_pick_date) {
            LocalDate now = selectedDate;
            DatePickerDialog dlg = new DatePickerDialog(
                    this,
                    (v, year, month, day) -> {
                        selectedDate = LocalDate.of(year, month + 1, day);
                        loadClasses();
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

    private void setupStudentOrChildrenFlow() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", "");

        if ("parent".equalsIgnoreCase(role)) {
            spinnerChildren.setVisibility(View.VISIBLE);

            String parentId = prefs.getString("userId",
                    prefs.getString("parentId",
                            prefs.getString("username", "")));

            childrenAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, new ArrayList<>());
            childrenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerChildren.setAdapter(childrenAdapter);

            loadChildren(parentId);

        } else {
            spinnerChildren.setVisibility(View.GONE);
            currentStudentId = prefs.getString("username", "");
            loadClasses();
        }
    }

    private void loadChildren(String parentId) {
        ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
        api.getChildrenByParentId(parentId).enqueue(new Callback<List<Student>>() {
            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> resp) {
                if (!resp.isSuccessful() || resp.body() == null) return;

                children.clear();
                children.addAll(resp.body());

                List<String> names = new ArrayList<>();
                for (Student s : children) names.add(s.getStudentName());

                childrenAdapter.clear();
                childrenAdapter.addAll(names);
                childrenAdapter.notifyDataSetChanged();

                if (!children.isEmpty())
                    currentStudentId = children.get(0).getStudentId();

                spinnerChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                        currentStudentId = children.get(pos).getStudentId();
                        loadClasses();
                    }
                    @Override public void onNothingSelected(AdapterView<?> parent) {}
                });

                loadClasses();
            }

            @Override
            public void onFailure(Call<List<Student>> call, Throwable t) {}
        });
    }

    // =============================================================
    // ★ 시간표 로딩 (학원 이름은 Adapter가 prefs에서 자동 표시)
    // =============================================================
    private void loadClasses() {
        if (currentStudentId == null || currentStudentId.isEmpty()) return;

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);

        api.getMyClasses(currentStudentId).enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                List<Course> all = response.body();
                if (all.isEmpty()) return;

                int dow = selectedDate.getDayOfWeek().getValue();

                List<Course> todays = new ArrayList<>();
                for (Course c : all) {
                    if (c.getDaysOfWeek() != null && c.getDaysOfWeek().contains(dow)) {
                        todays.add(c);
                    }
                }

                todays.sort(Comparator.comparing(Course::getStartTime));

                LocalDate today = LocalDate.now();
                LocalTime now = LocalTime.now();

                for (Course c : todays) {
                    LocalTime start = LocalTime.parse(c.getStartTime());
                    LocalTime end = LocalTime.parse(c.getEndTime());
                    String status;

                    if (selectedDate.isAfter(today)) status = "예정";
                    else if (selectedDate.isBefore(today)) status = "종료";
                    else {
                        if (now.isBefore(start)) status = "예정";
                        else if (now.isAfter(end)) status = "종료";
                        else status = "진행중";
                    }

                    c.setTodayStatus(status);
                }

                if (adapter == null) {
                    adapter = new TimetableAdapter(StudentTimetableActivity.this, todays);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.submit(todays);
                }
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {}
        });
    }

    private String safe(String v) { return v == null ? "" : v; }
}
