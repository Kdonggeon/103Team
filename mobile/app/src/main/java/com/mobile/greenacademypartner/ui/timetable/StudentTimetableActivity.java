package com.mobile.greenacademypartner.ui.timetable;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentTimetableActivity extends AppCompatActivity {

    public static final String EXTRA_OVERRIDE_STUDENT_ID = "overrideStudentId";

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private TimetableAdapter adapter;

    // ▼ 하단 네비 + 토글
    private BottomNavigationView bottomNavigation;
    private ImageButton btnHideNav, btnShowNav;

    // ▼ 자녀 스피너
    private Spinner spinnerChildren;
    private ArrayAdapter<String> childrenAdapter;
    private final List<Student> children = new ArrayList<>();
    private String currentStudentId = null;

    private final TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
    private final Locale loc = Locale.KOREA;
    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", loc);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_timetable);
        iso.setTimeZone(tz);

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
            if (id == R.id.nav_home) startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.nav_attendance) startActivity(new Intent(this, AttendanceActivity.class));
            else if (id == R.id.nav_qr) startActivity(new Intent(this, QRScannerActivity.class));
            else if (id == R.id.nav_my) startActivity(new Intent(this, MyPageActivity.class));
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
            Calendar now = Calendar.getInstance(tz, loc);
            new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar pick = Calendar.getInstance(tz, loc);
                        pick.set(year, month, dayOfMonth);
                        loadClassesForDate(pick);
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            ).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** 부모/학생에 따라 스피너 구성 */
    private void setupStudentOrChildrenFlow() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = safe(prefs.getString("role", "")).toLowerCase();
        Calendar today = Calendar.getInstance(tz, loc);

        if ("parent".equals(role)) {
            if (spinnerChildren != null) spinnerChildren.setVisibility(View.VISIBLE);

            String parentId = firstNonEmpty(
                    prefs.getString("userId", null),
                    prefs.getString("parentId", null),
                    prefs.getString("username", null)
            );

            if (parentId == null || parentId.isEmpty()) {
                Toast.makeText(this, "부모 ID가 없습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show();
                spinnerChildren.setVisibility(View.GONE);
                return;
            }

            childrenAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
            childrenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerChildren.setAdapter(childrenAdapter);

            loadChildrenAsync(parentId, () -> loadClassesForDate(today));

            spinnerChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && position < children.size()) {
                        Student s = children.get(position);
                        currentStudentId = safe(s.getStudentId());
                        loadClassesForDate(Calendar.getInstance(tz, loc));
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        } else {
            // 학생 로그인일 때
            spinnerChildren.setVisibility(View.GONE);
            currentStudentId = safe(prefs.getString("username", ""));
            if (currentStudentId.isEmpty()) {
                Toast.makeText(this, "학생 ID가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            loadClassesForDate(today);
        }
    }

    /** 부모의 자녀 목록 로딩 — 등록된 자녀만 표시 */
    private void loadChildrenAsync(String parentId, Runnable afterLoad) {
        try {
            ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
            api.getChildrenByParentId(parentId).enqueue(new Callback<List<Student>>() {
                @Override
                public void onResponse(Call<List<Student>> call, Response<List<Student>> resp) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        Toast.makeText(StudentTimetableActivity.this, "자녀 목록을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                        spinnerChildren.setVisibility(View.GONE);
                        return;
                    }

                    children.clear();
                    for (Student s : resp.body()) {
                        if (s == null) continue;
                        if (!safe(s.getStudentId()).isEmpty() && !safe(s.getStudentName()).isEmpty()) {
                            children.add(s);
                        }
                    }

                    if (children.isEmpty()) {
                        spinnerChildren.setVisibility(View.GONE);
                        Toast.makeText(StudentTimetableActivity.this, "등록된 자녀가 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> names = new ArrayList<>();
                    for (Student s : children) names.add(safe(s.getStudentName()));
                    childrenAdapter.clear();
                    childrenAdapter.addAll(names);
                    childrenAdapter.notifyDataSetChanged();

                    currentStudentId = safe(children.get(0).getStudentId());
                    spinnerChildren.setSelection(0);

                    if (afterLoad != null) afterLoad.run();
                }

                @Override
                public void onFailure(Call<List<Student>> call, Throwable t) {
                    Log.e("Timetable", "loadChildrenAsync failed", t);
                    spinnerChildren.setVisibility(View.GONE);
                    Toast.makeText(StudentTimetableActivity.this, "자녀 목록 조회 실패", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("Timetable", "loadChildrenAsync exception", e);
            spinnerChildren.setVisibility(View.GONE);
        }
    }

    /** 선택된 날짜(또는 오늘)의 요일에 맞춰 수업 필터 */
    private void loadClassesForDate(Calendar target) {
        if (currentStudentId == null || currentStudentId.trim().isEmpty()) {
            Toast.makeText(this, "자녀를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String studentId = currentStudentId.trim();
        int dowJavaUtil = target.get(Calendar.DAY_OF_WEEK);
        int dowMon1 = ((dowJavaUtil + 5) % 7) + 1;
        String dateIso = iso.format(target.getTime());

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
        api.getMyClasses(studentId).enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(StudentTimetableActivity.this, "수업 목록을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Course> filtered = new ArrayList<>();
                for (Course c : response.body()) {
                    if (c.getDaysOfWeek() != null && c.getDaysOfWeek().contains(dowMon1)) {
                        filtered.add(c);
                    }
                }

                filtered.sort(Comparator.comparing(
                        c -> c.getStartTime() != null ? c.getStartTime() : "",
                        String::compareTo
                ));

                if (adapter == null) {
                    adapter = new TimetableAdapter(StudentTimetableActivity.this, filtered);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.submit(filtered);
                }
                adapter.setDisplayDate(dateIso);

                if (filtered.isEmpty()) {
                    Toast.makeText(StudentTimetableActivity.this, "해당 날짜에 예정된 수업이 없습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                Log.e("StudentTimetable", "API 실패", t);
                Toast.makeText(StudentTimetableActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) if (v != null && !v.trim().isEmpty()) return v.trim();
        return null;
    }

    private String safe(String v) { return v == null ? "" : v; }
}
