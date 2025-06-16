package com.mobile.greenacademypartner.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import android.widget.LinearLayout;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.Attendance;
import com.mobile.greenacademypartner.ui.adapter.AttendanceAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClassAttendanceActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private Toolbar toolbar;
    private ListView listView;
    private String classId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_attendance);

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        listView = findViewById(R.id.attendance_list); // 수정된 부분

        setSupportActionBar(toolbar);
        ToolbarColorUtil.applyToolbarColor(this, toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, 1);

        classId = getIntent().getStringExtra("classId"); // 클래스 ID 받아옴
        loadAttendance(); // 반드시 호출
    }


    private void loadAttendance() {
        Log.d("ClassAttendance", "classId: " + classId);

        TeacherApi api = RetrofitClient.getClient().create(TeacherApi.class);
        api.getAttendanceByClassId(classId).enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                Log.d("ClassAttendance", "HTTP 상태: " + response.code());
                if (response.body() != null) {
                    Log.d("ClassAttendance", "응답 바디 크기: " + response.body().size());
                } else {
                    Log.d("ClassAttendance", "응답 바디: null");
                }
                Log.d("ClassAttendance", "응답 성공? " + response.isSuccessful());
                Log.d("ClassAttendance", "출석 수: " + (response.body() != null ? response.body().size() : 0));

                if (response.isSuccessful() && response.body() != null) {
                    AttendanceAdapter adapter = new AttendanceAdapter(ClassAttendanceActivity.this, response.body());
                    listView.setAdapter(adapter);
                } else {
                    Toast.makeText(ClassAttendanceActivity.this, "출석 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Attendance>> call, Throwable t) {
                Log.e("ClassAttendance", "출석 조회 실패: " + t.getMessage());
                Toast.makeText(ClassAttendanceActivity.this, "네트워크 오류 발생", Toast.LENGTH_SHORT).show();
            }
        });
    }

}

