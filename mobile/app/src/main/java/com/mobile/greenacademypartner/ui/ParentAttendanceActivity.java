package com.mobile.greenacademypartner.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.Attendance;
import com.mobile.greenacademypartner.ui.adapter.AttendanceAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParentAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ListView attendanceListView;
    private LinearLayout navContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_attendance); // ✅ 부모 전용 XML

        // 뷰 바인딩
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        toolbar = findViewById(R.id.toolbar);
        attendanceListView = findViewById(R.id.attendance_list_view);

        toolbar.setTitle("자녀 출석 조회");
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));

        setupToolbarAndDrawer();
        fetchAttendanceFromServer();
    }

    private void setupToolbarAndDrawer() {
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, 1); // ✅ 학부모 메뉴 index
    }

    private void fetchAttendanceFromServer() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String studentId = prefs.getString("username", null); // ✅ 학부모 로그인 시 자녀 ID 저장됨

        if (studentId == null) {
            Toast.makeText(this, "로그인 정보가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
        api.getAttendanceForStudent(studentId).enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Attendance> list = response.body();
                    Log.d("ParentAttendance", "출석 데이터 개수: " + list.size());
                    Log.d("출석요청", "studentId: " + studentId);

                    AttendanceAdapter adapter = new AttendanceAdapter(ParentAttendanceActivity.this, list);
                    attendanceListView.setAdapter(adapter);
                } else {
                    Toast.makeText(ParentAttendanceActivity.this, "출석 데이터를 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
                    Log.e("ParentAttendance", "응답 실패: " + response.code());
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
