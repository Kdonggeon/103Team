package com.mobile.greenacademypartner.ui.timetable;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.model.classes.Course;
import com.mobile.greenacademypartner.ui.adapter.TimetableAdapter;
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

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private RecyclerView recyclerView;
    private TimetableAdapter adapter;

    // 공통 포맷/타임존
    private final TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
    private final Locale   loc = Locale.KOREA;
    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", loc);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_timetable);
        iso.setTimeZone(tz);

        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("시간표");
        setSupportActionBar(toolbar);
        ThemeColorUtil.applyThemeColor(this, toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, 2);

        Button btnScanQr = findViewById(R.id.btn_scan_qr);
        btnScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(StudentTimetableActivity.this, QRScannerActivity.class);
            startActivity(intent);
        });

        recyclerView = findViewById(R.id.recycler_today_attendance);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 앱 진입 시: 오늘 날짜로 로드
        Calendar today = Calendar.getInstance(tz, loc);
        loadClassesForDate(today);
    }

    // 툴바 메뉴(캘린더 아이콘)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_student_timetable, menu);
        return true;
    }

    // 캘린더 클릭 → DatePickerDialog
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

    // 선택한 날짜(또는 오늘)의 요일에 맞춰 수업 필터
    private void loadClassesForDate(Calendar target) {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String studentId = prefs.getString("username", null);
        if (studentId == null) {
            Toast.makeText(this, "로그인 정보가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1=월 … 7=일 변환 (java.util.Calendar: 1=일 … 7=토)
        int dowJavaUtil = target.get(Calendar.DAY_OF_WEEK);
        int dowMon1 = ((dowJavaUtil + 5) % 7) + 1;

        String dateIso = iso.format(target.getTime()); // "yyyy-MM-dd" (어댑터의 날짜 표시에 사용)

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

                // 시간순 정렬
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
                // 어댑터에 선택 날짜 표시
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
}
