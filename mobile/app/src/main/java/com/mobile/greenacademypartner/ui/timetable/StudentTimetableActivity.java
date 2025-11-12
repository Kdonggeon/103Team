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
    private String currentStudentId = null;   // 스피너 선택(부모) or 로그인(학생)

    // 공통 포맷/타임존
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

        // ✅ 하단 네비 & 토글
        bottomNavigation = findViewById(R.id.bottom_navigation);
        btnHideNav = findViewById(R.id.btn_hide_nav);
        btnShowNav = findViewById(R.id.btn_show_nav);

        // ✅ 현재 화면이 시간표이므로 nav_timetable을 선택 상태로 고정
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
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_attendance) {
                startActivity(new Intent(this, AttendanceActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_qr) {
                // ★ QR 스캐너 열 때 studentId를 반드시 넘긴다
                String sid = (currentStudentId != null && !currentStudentId.trim().isEmpty())
                        ? currentStudentId.trim()
                        : null;

                if (sid == null || sid.isEmpty()) {
                    sid = resolveTargetStudentId();
                }
                if (sid == null || sid.isEmpty()) {
                    SharedPreferences sp1 = getSharedPreferences("login_prefs", MODE_PRIVATE);
                    SharedPreferences sp2 = getSharedPreferences("session", MODE_PRIVATE);
                    SharedPreferences sp3 = getSharedPreferences("login", MODE_PRIVATE);
                    sid = firstNonEmpty(
                            sp1 != null ? sp1.getString("username", null) : null,
                            sp1 != null ? sp1.getString("studentId", null) : null,
                            sp1 != null ? sp1.getString("Student_ID", null) : null,
                            sp2 != null ? sp2.getString("username", null) : null,
                            sp2 != null ? sp2.getString("studentId", null) : null,
                            sp2 != null ? sp2.getString("Student_ID", null) : null,
                            sp3 != null ? sp3.getString("username", null) : null,
                            sp3 != null ? sp3.getString("studentId", null) : null,
                            sp3 != null ? sp3.getString("Student_ID", null) : null
                    );
                }

                if (sid == null || sid.trim().isEmpty()) {
                    Toast.makeText(this, "학생 ID를 찾을 수 없습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show();
                    return true;
                }

                Log.d("StudentTimetable", "QR open with studentId=" + sid);
                Intent it = new Intent(this, QRScannerActivity.class);
                it.putExtra("studentId", sid);   // ★ 핵심
                startActivity(it);
                return true;
            } else if (id == R.id.nav_timetable) {
                return true; // 현재 화면
            } else if (id == R.id.nav_my) {
                startActivity(new Intent(this, MyPageActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // ✅ 스피너/학생ID 설정 후 오늘 수업 로드
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
            int y = now.get(Calendar.YEAR);
            int m = now.get(Calendar.MONTH);      // 0-based
            int d = now.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dlg = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar pick = Calendar.getInstance(tz, loc);
                        pick.set(Calendar.YEAR, year);
                        pick.set(Calendar.MONTH, month);
                        pick.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        loadClassesForDate(pick);
                    },
                    y, m, d
            );
            dlg.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** 부모/학생에 따라 스피너 구성 또는 숨김 */
    private void setupStudentOrChildrenFlow() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = safe(prefs.getString("role", "")).toLowerCase();
        Calendar today = Calendar.getInstance(tz, loc);

        // 1) overrideStudentId가 있으면 최우선
        String overrideId = getIntent() != null ? getIntent().getStringExtra(EXTRA_OVERRIDE_STUDENT_ID) : null;
        if (overrideId != null && !overrideId.trim().isEmpty()) {
            currentStudentId = overrideId.trim();
            if (spinnerChildren != null) spinnerChildren.setVisibility(View.GONE);
            loadClassesForDate(today);
            return;
        }

        if ("parent".equals(role)) {
            if (spinnerChildren != null) spinnerChildren.setVisibility(View.VISIBLE);

            String parentId = firstNonEmpty(
                    prefs.getString("parentId", null),
                    prefs.getString("userId", null),
                    prefs.getString("username", null)
            );
            if (parentId == null || parentId.trim().isEmpty()) {
                Toast.makeText(this, "부모 ID가 없습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show();
                if (spinnerChildren != null) spinnerChildren.setVisibility(View.GONE);
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
            if (spinnerChildren != null) spinnerChildren.setVisibility(View.GONE);
            currentStudentId = resolveTargetStudentId();
            if (currentStudentId == null || currentStudentId.trim().isEmpty()) {
                Toast.makeText(this, "학생 ID가 없습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            loadClassesForDate(today);
        }
    }

    private void loadChildrenAsync(String parentId, Runnable afterLoad) {
        try {
            ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
            Call<List<Student>> call = api.getChildrenByParentId(parentId);
            call.enqueue(new Callback<List<Student>>() {
                @Override
                public void onResponse(Call<List<Student>> call, Response<List<Student>> resp) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        Toast.makeText(StudentTimetableActivity.this, "자녀 목록을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    children.clear();
                    children.addAll(resp.body());

                    List<String> names = new ArrayList<>();
                    for (Student s : children) {
                        names.add(safe(s.getStudentName()));
                    }
                    childrenAdapter.clear();
                    childrenAdapter.addAll(names);
                    childrenAdapter.notifyDataSetChanged();

                    if (!children.isEmpty()) {
                        currentStudentId = safe(children.get(0).getStudentId());
                        spinnerChildren.setSelection(0);
                    } else {
                        currentStudentId = null;
                        Toast.makeText(StudentTimetableActivity.this, "등록된 자녀가 없습니다.", Toast.LENGTH_SHORT).show();
                    }

                    if (afterLoad != null) afterLoad.run();
                }

                @Override
                public void onFailure(Call<List<Student>> call, Throwable t) {
                    Log.e("Timetable", "loadChildrenAsync failed", t);
                    Toast.makeText(StudentTimetableActivity.this, "자녀 목록 조회 실패", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("Timetable", "loadChildrenAsync exception", e);
            Toast.makeText(this, "자녀 목록 처리 중 오류", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadClassesForDate(Calendar target) {
        String studentId = currentStudentId != null ? currentStudentId : resolveTargetStudentId();
        if (studentId == null || studentId.trim().isEmpty()) {
            Toast.makeText(this, "학생(자녀) 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            Log.e("StudentTimetable", "studentId is null/empty");
            return;
        }

        int dowJavaUtil = target.get(Calendar.DAY_OF_WEEK);
        int dowMon1 = ((dowJavaUtil + 5) % 7) + 1;

        String dateIso = iso.format(target.getTime());

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
        Call<List<Course>> call = api.getMyClasses(studentId);

        call.enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(StudentTimetableActivity.this, "수업 목록을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                    Log.e("StudentTimetable", "응답 실패 code=" + response.code());
                    return;
                }

                List<Course> filtered = new ArrayList<>();
                for (Course c : response.body()) {
                    List<Integer> dows = c.getDaysOfWeek();
                    if (dows != null && dows.contains(dowMon1)) {
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

    /** 여러 프리퍼런스/키에서 studentId를 복원 */
    private String resolveTargetStudentId() {
        // 0) override가 있으면 최우선
        Intent it = getIntent();
        if (it != null && it.hasExtra(EXTRA_OVERRIDE_STUDENT_ID)) {
            String s = it.getStringExtra(EXTRA_OVERRIDE_STUDENT_ID);
            if (s != null && !s.trim().isEmpty()) return s.trim();
            int v = it.getIntExtra(EXTRA_OVERRIDE_STUDENT_ID, -1);
            if (v != -1) return String.valueOf(v);
        }

        String[] prefNames = {"login_prefs", "session", "login"};
        String[] keys = {"username", "studentId", "Student_ID"};
        for (String p : prefNames) {
            try {
                SharedPreferences sp = getSharedPreferences(p, MODE_PRIVATE);
                for (String k : keys) {
                    String val = sp.getString(k, null);
                    if (val != null && !val.trim().isEmpty()) return val.trim();
                }
            } catch (Exception ignore) {}
        }
        return null;
    }

    private String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) return v;
        }
        return null;
    }

    private String safe(String v) { return v == null ? "" : v; }
}
