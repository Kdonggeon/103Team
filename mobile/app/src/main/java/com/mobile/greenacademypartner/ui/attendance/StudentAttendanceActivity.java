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
import com.mobile.greenacademypartner.api.AcademyApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.model.Academy;
import com.mobile.greenacademypartner.model.attendance.AttendanceResponse;
import com.mobile.greenacademypartner.model.timetable.SlotDto;
import com.mobile.greenacademypartner.ui.adapter.AttendanceAdapter;
import com.mobile.greenacademypartner.ui.main.MainActivity;
import com.mobile.greenacademypartner.ui.mypage.MyPageActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;
import com.mobile.greenacademypartner.ui.timetable.QRScannerActivity;
import com.mobile.greenacademypartner.ui.timetable.StudentTimetableActivity;

import org.json.JSONArray;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView listView;
    private AttendanceAdapter adapter;

    private TextView tvPresent, tvLate, tvAbsent;
    private LocalDate selectedDate = LocalDate.now();

    private BottomNavigationView bottomNav;
    private ImageButton btnHideNav, btnShowNav;

    private String studentId;

    // academyNumber → academyName
    private final Map<Integer, String> academyNameMap = new HashMap<>();
    private int currentAcademyNumber = -1;

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

        // ★ 학원 이름 먼저 로딩 → 그 다음 slot + 출석 병합
        loadAcademyNamesThenSlots();
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

        selectedDate = LocalDate.now();

        try {
            JSONArray arr = new JSONArray(prefs.getString("academyNumbers", "[]"));
            if (arr.length() > 0)
                currentAcademyNumber = arr.getInt(0);
        } catch (Exception ignored) {}
    }

    /** ★ 학원명 먼저 로드 */
    private void loadAcademyNamesThenSlots() {
        AcademyApi api = RetrofitClient.getClient().create(AcademyApi.class);

        api.getAcademyList().enqueue(new Callback<List<Academy>>() {
            @Override
            public void onResponse(Call<List<Academy>> call, Response<List<Academy>> response) {
                academyNameMap.clear();

                if (response.isSuccessful() && response.body() != null) {
                    for (Academy a : response.body()) {
                        academyNameMap.put(a.getAcademyNumber(), a.getAcademyName());
                    }
                }
                loadSlotsForSelectedDate();
            }

            @Override public void onFailure(Call<List<Academy>> call, Throwable t) {
                loadSlotsForSelectedDate();
            }
        });
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
                        loadSlotsForSelectedDate();
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

    /** ★ SlotDto → 날짜 필터링 (시간은 Slot에서 그대로 사용) */
    private void loadSlotsForSelectedDate() {

        if (studentId == null) return;

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
        String weekStart = getWeekStart(selectedDate);

        api.getTimetable(studentId, weekStart, 7).enqueue(new Callback<List<SlotDto>>() {
            @Override
            public void onResponse(Call<List<SlotDto>> call, Response<List<SlotDto>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(StudentAttendanceActivity.this, "시간표 조회 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<SlotDto> all = response.body();
                List<SlotDto> todays = new ArrayList<>();
                String targetYmd = selectedDate.toString();

                for (SlotDto s : all) {
                    if (!s.date.equals(targetYmd)) continue;

                    // 학원이름 세팅
                    if (s.academyNumber != null) {
                        s.academyName = academyNameMap.get(s.academyNumber);
                    }

                    // ★ startTime, endTime은 SlotDto에 있는 그대로 사용
                    todays.add(s);
                }

                loadAttendanceAndMerge(todays);
            }

            @Override
            public void onFailure(Call<List<SlotDto>> call, Throwable t) {
                Toast.makeText(StudentAttendanceActivity.this, "시간표 조회 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** ★ SlotDto + Attendance 병합 (시간은 Slot 기준) */
    private void loadAttendanceAndMerge(List<SlotDto> slots) {

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);

        api.getAttendanceForStudent(studentId).enqueue(new Callback<List<AttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(StudentAttendanceActivity.this, "출석 조회 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<AttendanceResponse> attendList = response.body();
                List<AttendanceResponse> finalList = new ArrayList<>();

                long present = 0, late = 0, absent = 0;

                LocalDate today = LocalDate.now();
                LocalTime now = LocalTime.now();

                for (SlotDto s : slots) {

                    AttendanceResponse matched = null;

                    // className + 날짜로 출석 기록 찾기
                    for (AttendanceResponse ar : attendList) {
                        if (cleanClassName(ar.getClassName())
                                .equals(cleanClassName(s.className))
                                && ar.getDate() != null
                                && ar.getDate().startsWith(s.date)) {
                            matched = ar;
                            break;
                        }
                    }

                    AttendanceResponse item;

                    if (matched != null) {
                        item = matched;
                        // ★ Slot의 시간으로 통일
                        item.setStartTime(s.startTime);
                        item.setEndTime(s.endTime);

                    } else {

                        item = new AttendanceResponse();
                        item.setClassName(s.className);
                        item.setStartTime(s.startTime);
                        item.setEndTime(s.endTime);
                        item.setDate(s.date);
                        item.setAcademyName(s.academyName);

                        String start = s.startTime;
                        String end = s.endTime;

                        if (start == null || end == null) {
                            if (selectedDate.isAfter(today)) item.setStatus("예정");
                            else if (selectedDate.isBefore(today)) item.setStatus("결석");
                            else item.setStatus("결석");  // 오늘인데 시간 없음 → 결석
                        } else {

                            LocalTime tStart = LocalTime.parse(start);
                            LocalTime tEnd = LocalTime.parse(end);

                            if (selectedDate.isAfter(today)) item.setStatus("예정");
                            else if (selectedDate.isBefore(today)) item.setStatus("결석");
                            else if (now.isBefore(tStart)) item.setStatus("예정");
                            else if (now.isAfter(tEnd)) item.setStatus("결석");
                            else item.setStatus("수업중");
                        }
                    }

                    if ("출석".equals(item.getStatus())) present++;
                    else if ("지각".equals(item.getStatus())) late++;
                    else if ("결석".equals(item.getStatus())) absent++;

                    finalList.add(item);
                }

                // 시간순 정렬 (Slot에서 온 startTime 기준)
                finalList.sort((a, b) -> {
                    if (a.getStartTime() == null) return 1;
                    if (b.getStartTime() == null) return -1;
                    return a.getStartTime().compareTo(b.getStartTime());
                });

                adapter.setAll(finalList);

                tvPresent.setText("출석 " + present);
                tvLate.setText("지각 " + late);
                tvAbsent.setText("결석 " + absent);
            }

            @Override
            public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                Toast.makeText(StudentAttendanceActivity.this, "출석 조회 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** ★ className에서 "(학원명)" 제거 */
    private String cleanClassName(String name) {
        if (name == null) return null;
        int idx = name.indexOf("(");
        if (idx > 0) return name.substring(0, idx).trim();
        return name.trim();
    }

    /** 날짜가 속한 월요일 */
    private String getWeekStart(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        LocalDate monday = date.minusDays(dow.getValue() - 1);
        return monday.toString();
    }
}
