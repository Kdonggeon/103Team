package com.mobile.greenacademypartner.ui.attendance;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.menu.ToolbarIconUtil;
import com.mobile.greenacademypartner.model.teacher.TeacherAttendance;
import com.mobile.greenacademypartner.ui.adapter.TeacherAttendanceAdapter;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClassAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private String classId;
    private String date = "2025-06-17"; // 테스트용 하드코딩

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_attendance);

        classId = getIntent().getStringExtra("classId");

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        recyclerView = findViewById(R.id.recycler_attendance);


        setSupportActionBar(toolbar);


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState(); // ← 햄버거 버튼 표시하는 핵심
        ToolbarIconUtil.applyWhiteIcons(toolbar, toggle);

        setTitle("출석 상세");

        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        NavigationMenuHelper.setupMenu(this, findViewById(R.id.nav_container), drawerLayout, null, 1);

        fetchAttendanceData();
        ThemeColorUtil.applyThemeColor(this, toolbar);
    }

    private void fetchAttendanceData() {
        TeacherApi api = RetrofitClient.getClient().create(TeacherApi.class);
        api.getAttendanceForClass(classId, date).enqueue(new Callback<List<TeacherAttendance>>() {
            @Override
            public void onResponse(Call<List<TeacherAttendance>> call, Response<List<TeacherAttendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TeacherAttendance> list = response.body();
                    recyclerView.setLayoutManager(new LinearLayoutManager(ClassAttendanceActivity.this));
                    recyclerView.setAdapter(new TeacherAttendanceAdapter(list)); // ✅ 이거 하나만!
                } else {
                    Toast.makeText(ClassAttendanceActivity.this, "출석 데이터 없음", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<TeacherAttendance>> call, Throwable t) {
                Toast.makeText(ClassAttendanceActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
                Log.e("ClassAttendance", "출석 조회 실패", t);
            }
        });
    }




}
