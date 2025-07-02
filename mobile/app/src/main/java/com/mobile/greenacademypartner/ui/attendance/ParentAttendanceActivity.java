package com.mobile.greenacademypartner.ui.attendance;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.attendance.Attendance;
import com.mobile.greenacademypartner.ui.adapter.ParentAttendanceAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParentAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private RecyclerView attendanceListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_attendance);

        // ✅ 뷰 초기화
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        attendanceListView = findViewById(R.id.attendance_list_view);

        setupToolbarAndDrawer();
        fetchChildAttendance();
    }

    private void setupToolbarAndDrawer() {
        setSupportActionBar(toolbar);
        setTitle("자녀 출석 확인");

        ToolbarColorUtil.applyToolbarColor(this, toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationMenuHelper.setupMenu(
                this,
                navContainer,
                drawerLayout,
                null,
                1 // 출석 메뉴 인덱스
        );
    }

    private void fetchChildAttendance() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String childId = prefs.getString("childStudentId", null);

        if (childId == null) {
            Toast.makeText(this, "자녀 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            Log.e("ParentAttendance", "❌ childStudentId is null");
            return;
        }

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
        api.getAttendanceForStudent(childId).enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Attendance> list = response.body();
                    Log.d("ParentAttendance", "자녀 출석 수: " + list.size());

                    // ✅ RecyclerView 설정
                    attendanceListView.setLayoutManager(new LinearLayoutManager(ParentAttendanceActivity.this));
                    ParentAttendanceAdapter adapter = new ParentAttendanceAdapter(ParentAttendanceActivity.this, list);
                    attendanceListView.setAdapter(adapter);
                } else {
                    Toast.makeText(ParentAttendanceActivity.this, "출석 조회 실패", Toast.LENGTH_SHORT).show();
                    Log.e("ParentAttendance", "응답 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Attendance>> call, Throwable t) {
                Toast.makeText(ParentAttendanceActivity.this, "서버 오류 발생", Toast.LENGTH_SHORT).show();
                Log.e("ParentAttendance", "API 호출 실패", t);
            }
        });
    }
}
