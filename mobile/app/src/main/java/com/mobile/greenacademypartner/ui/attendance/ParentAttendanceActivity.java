// ParentAttendanceActivity.java
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
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

public class ParentAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView attendanceListView;
    private Spinner spinnerChildren;

    // 요약 카드
    private TextView tvPresent, tvLate, tvAbsent;

    private SharedPreferences prefs;
    private StudentApi studentApi;
    private ParentApi parentApi;

    private AttendanceAdapter adapter;
    private final Map<String, List<Integer>> classDowMap = new HashMap<>();
    private final List<AttendanceResponse> allAttendances = new ArrayList<>();

    private volatile String activeChildId = null;
    private Call<List<Course>> callClasses;
    private Call<List<AttendanceResponse>> callAttendance;

    // 요일 하이라이트(어댑터가 쓰는 값, 선택 날짜 변경 시 갱신)
    private int currentDisplayDow = -1;

    // ✅ 선택 날짜(기본: 오늘) & 시간대/표시 포맷
    private LocalDate selectedDate = LocalDate.now();
    private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter KEY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.KOREA);

    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_PARENT_ID = "parentId";
    private static final String KEY_PARENT_ID_FALLBACK = "username";
    private static final String KEY_LAST_SELECTED = "last_selected_child_id";

    private static class ChildItem {
        final String id;
        final String name;
        ChildItem(String id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    private static final TimeZone KST = TimeZone.getTimeZone("Asia/Seoul");
    private static final Locale KOR = Locale.KOREA;

    // 네비게이션 토글 버튼
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
        tvLate    = findViewById(R.id.tv_late_count);
        tvAbsent  = findViewById(R.id.tv_absent_count);

        int white = ContextCompat.getColor(this, android.R.color.white);
        toolbar.setTitleTextColor(white);
        setSupportActionBar(toolbar);
        setTitle("자녀 출석 확인");
        ThemeColorUtil.applyThemeColor(this, toolbar);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        studentApi = RetrofitClient.getClient().create(StudentApi.class);
        parentApi  = RetrofitClient.getClient().create(ParentApi.class);

        attendanceListView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(this, new ArrayList<>());
        attendanceListView.setAdapter(adapter);

        currentDisplayDow = getTodayDowMon1ToSun7();

        // ✅ MainActivity처럼 userId도 확인하게 수정
        String parentId = prefs.getString("userId", null);
        if (parentId == null || parentId.isEmpty())
            parentId = prefs.getString(KEY_PARENT_ID, null);
        if (parentId == null || parentId.isEmpty())
            parentId = prefs.getString(KEY_PARENT_ID_FALLBACK, null);


        if (parentId == null || parentId.isEmpty()) {
            Toast.makeText(this, "학부모 로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchChildrenAndBind(parentId);

        // 하단 네비 + 토글
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
    }

    /** 자녀 목록 조회 및 스피너 바인딩 */
    private void fetchChildrenAndBind(String parentId) {
        parentApi.getChildrenByParentId(parentId).enqueue(new Callback<List<Student>>() {
            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    spinnerChildren.setEnabled(false);
                    spinnerChildren.setAdapter(new ArrayAdapter<>(ParentAttendanceActivity.this,
                            android.R.layout.simple_spinner_item, new String[]{"자녀 없음"}));
                    return;
                }

                List<Student> students = response.body();
                List<ChildItem> items = new ArrayList<>();
                if (students != null) {
                    for (Student s : students) {
                        if (s == null) continue;
                        String id = s.getStudentId();
                        String name = s.getStudentName();
                        if (id != null && !id.isEmpty()) {
                            items.add(new ChildItem(id, (name == null || name.isEmpty()) ? id : name));
                        }
                    }
                }

                if (items.isEmpty()) {
                    spinnerChildren.setEnabled(false);
                    spinnerChildren.setAdapter(new ArrayAdapter<>(ParentAttendanceActivity.this,
                            android.R.layout.simple_spinner_item, new String[]{"자녀 없음"}));
                    return;
                }

                ArrayAdapter<ChildItem> adapterSpinner = new ArrayAdapter<>(ParentAttendanceActivity.this,
                        android.R.layout.simple_spinner_item, items);
                adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerChildren.setAdapter(adapterSpinner);
                spinnerChildren.setEnabled(true);

                String lastSelectedId = prefs.getString(KEY_LAST_SELECTED, null);
                if (lastSelectedId != null) {
                    int pos = findPositionById(items, lastSelectedId);
                    if (pos >= 0) spinnerChildren.setSelection(pos);
                }

                spinnerChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                        ChildItem it = (ChildItem) parent.getItemAtPosition(position);
                        if (it.id != null && !it.id.isEmpty()) {
                            prefs.edit().putString(KEY_LAST_SELECTED, it.id).apply();
                            onSelectChild(it.id);
                        }
                    }
                    @Override public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            @Override
            public void onFailure(Call<List<Student>> call, Throwable t) {
                spinnerChildren.setEnabled(false);
                spinnerChildren.setAdapter(new ArrayAdapter<>(ParentAttendanceActivity.this,
                        android.R.layout.simple_spinner_item, new String[]{"자녀 없음"}));
            }
        });
    }

    private void onSelectChild(String studentId) {
        activeChildId = studentId;
        if (callClasses != null) callClasses.cancel();
        if (callAttendance != null) callAttendance.cancel();

        adapter.setAll(new ArrayList<>());
        adapter.setClassDowMap(new HashMap<>());
        adapter.setDisplayDow(currentDisplayDow);
        clearSummary();

        fetchClassesThenAttendance(studentId);
    }

    private void fetchClassesThenAttendance(String studentId) {
        callClasses = studentApi.getMyClasses(studentId);
        callClasses.enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                if (call.isCanceled() || !studentId.equals(activeChildId)) return;

                classDowMap.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (Course c : response.body()) {
                        if (c == null) continue;
                        String key = norm(c.getClassName());
                        List<Integer> dows = c.getDaysOfWeek();
                        if (!key.isEmpty() && dows != null && !dows.isEmpty()) {
                            classDowMap.put(key, dows);
                        }
                    }
                }
                adapter.setClassDowMap(classDowMap);
                adapter.setDisplayDow(currentDisplayDow);
                fetchAttendanceFromServer(studentId);
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                if (call.isCanceled() || !studentId.equals(activeChildId)) return;
                adapter.setClassDowMap(classDowMap);
                adapter.setDisplayDow(currentDisplayDow);
                fetchAttendanceFromServer(studentId);
            }
        });
    }

    private void fetchAttendanceFromServer(String studentId) {
        callAttendance = studentApi.getAttendanceForStudent(studentId);
        callAttendance.enqueue(new Callback<List<AttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> response) {
                if (call.isCanceled() || !studentId.equals(activeChildId)) return;

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ParentAttendanceActivity.this, "출석 조회 실패(" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    adapter.setAll(new ArrayList<>());
                    clearSummary();
                    return;
                }

                List<AttendanceResponse> list = response.body();
                if (list == null) list = new ArrayList<>();
                try {
                    Collections.sort(list, Comparator.comparing(AttendanceResponse::getDate, String::compareTo));
                } catch (Exception ignore) {}

                allAttendances.clear();
                allAttendances.addAll(list);

                // ✅ 전체를 보관해두고, 현재 선택 날짜로 화면 반영
                applyDateFilterAndBind();
            }

            @Override
            public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                if (call.isCanceled() || !studentId.equals(activeChildId)) return;
                Toast.makeText(ParentAttendanceActivity.this, "출석 조회 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                adapter.setAll(new ArrayList<>());
                clearSummary();
            }
        });
    }

    // ─────────────────────────────
    // 메뉴(툴바 아이콘) : 캘린더로 날짜 선택
    // ─────────────────────────────
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_student_timetable, menu); // action_pick_date 있어야 함
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

                    // 어댑터의 요일 하이라이트도 갱신(옵션)
                    int dow = selectedDate.getDayOfWeek().getValue(); // Mon=1..Sun=7
                    currentDisplayDow = dow;
                    adapter.setDisplayDow(dow);

                    // ✅ 실제 필터 적용
                    applyDateFilterAndBind();
                },
                y, m, d
        );
        dlg.show();
    }

    // ─────────────────────────────
    // 날짜 필터링 & 요약 갱신
    // ─────────────────────────────
    /** 선택 날짜 기준으로 리스트/카운트 반영 */
    private void applyDateFilterAndBind() {
        List<AttendanceResponse> filtered = new ArrayList<>();
        long present = 0, late = 0, absent = 0;

        for (AttendanceResponse ar : allAttendances) {
            if (isSameSelectedDay(ar)) {
                filtered.add(ar);
                String s = normalizeStatus(ar.getStatus());
                if ("출석".equals(s)) present++;
                else if ("지각".equals(s)) late++;
                else if ("결석".equals(s)) absent++;
            }
        }

        adapter.setAll(filtered);
        tvPresent.setText("출석 " + present);
        tvLate.setText("지각 " + late);
        tvAbsent.setText("결석 " + absent);
    }

    /** AttendanceResponse에서 날짜 문자열 추출 (프로젝트에 맞게 필요한 항목만 남겨도 됨) */
    private String pickDateString(AttendanceResponse ar) {
        return (ar != null) ? ar.getDate() : null;
    }


    /** 아이템이 선택 날짜와 같은 날인지 (KST 기준 안전 비교) */
    private boolean isSameSelectedDay(AttendanceResponse ar) {
        String raw = pickDateString(ar);
        if (raw == null || raw.isEmpty()) return false;

        // 1) "yyyy-MM-dd" 형태면 앞 10자리만 비교
        if (raw.length() >= 10) {
            try {
                LocalDate d = LocalDate.parse(raw.substring(0, 10));
                return d.equals(selectedDate);
            } catch (Exception ignored) {}
        }

        // 2) ISO-8601(+Z/+09:00/.SSS) 등 → KST로 변환 후 날짜 비교
        try { return Instant.parse(raw).atZone(ZONE_KST).toLocalDate().equals(selectedDate); } catch (Exception ignored) {}
        try { return OffsetDateTime.parse(raw).atZoneSameInstant(ZONE_KST).toLocalDate().equals(selectedDate); } catch (Exception ignored) {}

        // 3) 마지막 시도
        try { return LocalDate.parse(raw).equals(selectedDate); } catch (Exception ignored) {}

        return false;
    }

    // ─────────────────────────────
    // 기존 유틸/요약
    // ─────────────────────────────
    private void updateSummaryCountsForDow(int dowMon1ToSun7) {
        long present = 0, late = 0, absent = 0;
        for (AttendanceResponse ar : allAttendances) {
            String s = normalizeStatus(ar != null ? ar.getStatus() : null);
            if ("출석".equals(s)) present++;
            else if ("지각".equals(s)) late++;
            else if ("결석".equals(s)) absent++;
        }
        tvPresent.setText("출석 " + present);
        tvLate.setText("지각 " + late);
        tvAbsent.setText("결석 " + absent);
    }

    private void clearSummary() {
        tvPresent.setText("출석 0");
        tvLate.setText("지각 0");
        tvAbsent.setText("결석 0");
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

    private int getTodayDowMon1ToSun7() {
        Calendar cal = Calendar.getInstance(KST, KOR);
        int c = cal.get(Calendar.DAY_OF_WEEK);
        return (c == Calendar.SUNDAY) ? 7 : (c - 1);
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private int findPositionById(List<ChildItem> list, String id) {
        for (int i = 0; i < list.size(); i++) if (id.equals(list.get(i).id)) return i;
        return -1;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (toolbar != null) {
            ThemeColorUtil.applyThemeColor(this, toolbar);
        }
    }
}
