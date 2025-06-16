package com.mobile.greenacademypartner.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.model.Attendance;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.ui.adapter.AttendanceAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParentAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private ListView attendanceListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance); // ✅ 동일 XML 사용

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        attendanceListView = findViewById(R.id.attendance_list_view);

        setupToolbarAndDrawer();
        fetchChildAttendance();
    }

    private void setupToolbarAndDrawer() {
        setTitle("자녀 출석 조회");
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, 1);
    }

    private void fetchChildAttendance() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String studentId = prefs.getString("username", null); // ✅ 자녀 ID가 username에 저장됨

        if (studentId == null) {
            Toast.makeText(this, "자녀 정보가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
        api.getAttendanceForChild(studentId).enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Attendance> list = response.body();
                    Log.d("Attendance", "출석 데이터 개수: " + list.size());

                    AttendanceAdapter adapter = new AttendanceAdapter(ParentAttendanceActivity.this, list);
                    attendanceListView.setAdapter(adapter);
                } else {
                    Toast.makeText(ParentAttendanceActivity.this, "출석 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Attendance>> call, Throwable t) {
                Toast.makeText(ParentAttendanceActivity.this, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ParentAttendance", "API 실패", t);
            }
        });
    }
}
