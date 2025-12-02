package com.mobile.greenacademypartner.ui.timetable;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.AcademyApi;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.model.Academy;
import com.mobile.greenacademypartner.model.student.Student;
import com.mobile.greenacademypartner.model.timetable.SlotDto;
import com.mobile.greenacademypartner.ui.adapter.TimetableSlotAdapter;
import com.mobile.greenacademypartner.ui.attendance.AttendanceActivity;
import com.mobile.greenacademypartner.ui.main.MainActivity;
import com.mobile.greenacademypartner.ui.mypage.MyPageActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import org.json.JSONArray;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentTimetableActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private RecyclerView recyclerView;
    private TimetableSlotAdapter adapter;

    private BottomNavigationView bottomNavigation;
    private ImageButton btnHideNav, btnShowNav;

    private Spinner spinnerChildren;
    private ArrayAdapter<String> childrenAdapter;
    private final List<Student> children = new ArrayList<>();

    private String currentStudentId = null;
    private LocalDate selectedDate = LocalDate.now();

    // 학원 번호 → 학원명
    private final Map<Integer, String> academyNameMap = new HashMap<>();
    private int currentAcademyNumber = -1;

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
                        loadSlots();  // 날짜 바뀌면 슬롯 다시 로드
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

        // 학원번호 불러오기
        List<Integer> academyNumbers = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString("academyNumbers", "[]"));
            for (int i = 0; i < arr.length(); i++) academyNumbers.add(arr.getInt(i));
        } catch (Exception ignore) {}

        if (!academyNumbers.isEmpty()) currentAcademyNumber = academyNumbers.get(0);

        if ("parent".equalsIgnoreCase(role)) {

            spinnerChildren.setVisibility(View.VISIBLE);

            String parentId = prefs.getString(
                    "userId",
                    prefs.getString("parentId", prefs.getString("username", ""))
            );

            childrenAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    new ArrayList<>()
            );
            childrenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerChildren.setAdapter(childrenAdapter);

            loadChildren(parentId);

        } else {

            spinnerChildren.setVisibility(View.GONE);
            currentStudentId = prefs.getString("username", "");

            loadAcademyNamesThenSlots();
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
                for (Student c : children) names.add(c.getStudentName());

                childrenAdapter.clear();
                childrenAdapter.addAll(names);
                childrenAdapter.notifyDataSetChanged();

                if (!children.isEmpty())
                    currentStudentId = children.get(0).getStudentId();

                spinnerChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                        currentStudentId = children.get(pos).getStudentId();
                        loadAcademyNamesThenSlots();
                    }
                    @Override public void onNothingSelected(AdapterView<?> parent) {}
                });

                loadAcademyNamesThenSlots();
            }

            @Override
            public void onFailure(Call<List<Student>> call, Throwable t) {}
        });
    }

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

                loadSlots();
            }

            @Override
            public void onFailure(Call<List<Academy>> call, Throwable t) {
                loadSlots();
            }
        });
    }

    /** ▣ slot API 기반 시간표 로딩 */
    private void loadSlots() {

        if (currentStudentId == null || currentStudentId.isEmpty()) return;

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);

        String weekStart = getWeekStart(selectedDate);

        api.getTimetable(currentStudentId, weekStart, 7).enqueue(new Callback<List<SlotDto>>() {
            @Override
            public void onResponse(Call<List<SlotDto>> call, Response<List<SlotDto>> response) {

                if (!response.isSuccessful() || response.body() == null) return;

                List<SlotDto> all = response.body();

                List<SlotDto> todays = new ArrayList<>();
                String targetYmd = selectedDate.toString();   // ★ 중요

                for (SlotDto s : all) {
                    if (s.date.equals(targetYmd)) {

                        if (s.academyNumber != null) {
                            String nm = academyNameMap.get(s.academyNumber);
                            if (nm != null) s.academyName = nm;
                        }

                        todays.add(s);
                    }
                }

                if (adapter == null) {
                    adapter = new TimetableSlotAdapter(todays);
                    adapter.setDisplayDate(targetYmd); // ★ 추가
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.submit(todays);
                    adapter.setDisplayDate(targetYmd); // ★ 추가
                }
            }

            @Override
            public void onFailure(Call<List<SlotDto>> call, Throwable t) { }
        });
    }

    /** ▣ 해당 날짜의 속한 월요일 계산 */
    private String getWeekStart(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        LocalDate monday = date.minusDays(dow.getValue() - 1);
        return monday.toString();
    }
}
