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
import com.mobile.greenacademypartner.api.AcademyApi;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.model.Academy;
import com.mobile.greenacademypartner.model.attendance.AttendanceResponse;
import com.mobile.greenacademypartner.model.student.Student;
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

public class ParentAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView attendanceListView;
    private Spinner spinnerChildren;

    private TextView tvPresent, tvLate, tvAbsent;

    private AttendanceAdapter adapter;

    private ParentApi parentApi;
    private StudentApi studentApi;
    private AcademyApi academyApi;

    private List<Student> childList = new ArrayList<>();
    private Student selectedChild;

    private LocalDate selectedDate = LocalDate.now();

    private BottomNavigationView bottomNav;
    private ImageButton btnHideNav, btnShowNav;

    // academyNumber → academyName
    private final Map<Integer, String> academyNameMap = new HashMap<>();

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

        parentApi = RetrofitClient.getClient().create(ParentApi.class);
        studentApi = RetrofitClient.getClient().create(StudentApi.class);
        academyApi = RetrofitClient.getClient().create(AcademyApi.class);

        setupBottomNav();

        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);

        String parentId = prefs.getString("username",
                prefs.getString("parentId",
                        prefs.getString("userId", null)));

        if (parentId == null) {
            Toast.makeText(this, "학부모 로그인 정보 없음", Toast.LENGTH_SHORT).show();
            return;
        }

        loadAcademyNames(parentId);
    }

    private void loadAcademyNames(String parentId) {

        academyApi.getAcademyList().enqueue(new Callback<List<Academy>>() {
            @Override
            public void onResponse(Call<List<Academy>> call, Response<List<Academy>> response) {

                academyNameMap.clear();

                if (response.isSuccessful() && response.body() != null) {
                    for (Academy a : response.body()) {
                        academyNameMap.put(a.getAcademyNumber(), a.getAcademyName());
                    }
                }

                fetchChildren(parentId);
            }

            @Override public void onFailure(Call<List<Academy>> call, Throwable t) {
                fetchChildren(parentId);
            }
        });
    }

    private void fetchChildren(String parentId) {

        parentApi.getChildrenByParentId(parentId).enqueue(new Callback<List<Student>>() {
            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    spinnerChildren.setAdapter(new ArrayAdapter<>(
                            ParentAttendanceActivity.this,
                            android.R.layout.simple_spinner_item,
                            new String[]{"자녀 없음"}
                    ));
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
                        loadSlots();  // ★ Slot 기반 로딩
                    }

                    @Override public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            @Override public void onFailure(Call<List<Student>> call, Throwable t) {}
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
                        if (selectedChild != null) loadSlots();
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

    /** ★ SlotDto 기반 수업 불러오기 */
    private void loadSlots() {

        if (selectedChild == null) return;

        studentApi.getTimetable(
                selectedChild.getStudentId(),
                getWeekStart(selectedDate),
                7
        ).enqueue(new Callback<List<SlotDto>>() {
            @Override
            public void onResponse(Call<List<SlotDto>> call, Response<List<SlotDto>> response) {

                if (!response.isSuccessful() || response.body() == null) return;

                List<SlotDto> todaySlots = new ArrayList<>();
                String target = selectedDate.toString();

                for (SlotDto s : response.body()) {
                    if (!s.date.equals(target)) continue;

                    if (s.academyNumber != null)
                        s.academyName = academyNameMap.get(s.academyNumber);

                    todaySlots.add(s);
                }

                loadAttendance(todaySlots);
            }

            @Override public void onFailure(Call<List<SlotDto>> call, Throwable t) {}
        });
    }

    /** ★ Slot + Attendance 병합 */
    private void loadAttendance(List<SlotDto> slots) {

        studentApi.getAttendanceForStudent(selectedChild.getStudentId())
                .enqueue(new Callback<List<AttendanceResponse>>() {
                    @Override
                    public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> response) {

                        if (!response.isSuccessful() || response.body() == null) return;

                        List<AttendanceResponse> attend = response.body();
                        List<AttendanceResponse> finalList = new ArrayList<>();

                        long present = 0, late = 0, absent = 0;

                        LocalDate today = LocalDate.now();
                        LocalTime now = LocalTime.now();

                        for (SlotDto s : slots) {

                            AttendanceResponse matched = null;

                            for (AttendanceResponse ar : attend) {

                                if (clean(s.className).equals(clean(ar.getClassName()))
                                        && ar.getDate() != null
                                        && ar.getDate().startsWith(s.date)) {
                                    matched = ar;
                                    break;
                                }
                            }

                            AttendanceResponse item;

                            if (matched != null) {
                                item = matched;
                                item.setStartTime(s.startTime);
                                item.setEndTime(s.endTime);
                            } else {

                                item = new AttendanceResponse();
                                item.setClassName(s.className);
                                item.setDate(s.date);
                                item.setStartTime(s.startTime);
                                item.setEndTime(s.endTime);
                                item.setAcademyName(s.academyName);

                                if (s.startTime == null || s.endTime == null) {
                                    if (selectedDate.isAfter(today)) item.setStatus("예정");
                                    else if (selectedDate.isBefore(today)) item.setStatus("결석");
                                    else item.setStatus("결석");
                                } else {

                                    LocalTime st = LocalTime.parse(s.startTime);
                                    LocalTime en = LocalTime.parse(s.endTime);

                                    if (selectedDate.isAfter(today)) item.setStatus("예정");
                                    else if (selectedDate.isBefore(today)) item.setStatus("결석");
                                    else if (now.isBefore(st)) item.setStatus("예정");
                                    else if (now.isAfter(en)) item.setStatus("결석");
                                    else item.setStatus("수업중");
                                }
                            }

                            if ("출석".equals(item.getStatus())) present++;
                            else if ("지각".equals(item.getStatus())) late++;
                            else if ("결석".equals(item.getStatus())) absent++;

                            finalList.add(item);
                        }

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

                    @Override public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {}
                });
    }

    /** className 정리 */
    private String clean(String n) {
        if (n == null) return "";
        int pos = n.indexOf("(");
        return (pos > 0 ? n.substring(0, pos) : n).trim();
    }

    private String getWeekStart(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return d.minusDays(dow.getValue() - 1).toString();
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
}
