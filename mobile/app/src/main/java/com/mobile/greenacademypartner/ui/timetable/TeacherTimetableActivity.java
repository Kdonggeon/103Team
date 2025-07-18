package com.mobile.greenacademypartner.ui.timetable;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.model.classes.Course;
import com.mobile.greenacademypartner.model.teacher.TeacherClass;
import com.mobile.greenacademypartner.ui.adapter.TeacherClassAdapter;
import com.mobile.greenacademypartner.ui.attendance.ClassAttendanceActivity;
import com.mobile.greenacademypartner.ui.classes.CreateClassActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeacherTimetableActivity extends AppCompatActivity
        implements TeacherClassAdapter.OnClassClickListener {

    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private RecyclerView recyclerView;
    private TeacherClassAdapter adapter;
    private FloatingActionButton fabAdd;
    private TeacherApi api;
    private String teacherId;
    private String today;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_timetable);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("오늘의 수업");

        // ✅ 이 시점에 바로 테마 색상 적용!
        ThemeColorUtil.applyThemeColor(this, toolbar);

        // 드로어 및 사이드 메뉴 설정
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, 2);

        // RecyclerView 및 FAB 설정
        recyclerView = findViewById(R.id.recycler_today_classes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fabAdd = findViewById(R.id.fab_add_class);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateClassActivity.class);
            startActivity(intent);
        });

        // 로그인 정보 및 오늘 날짜
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        teacherId = prefs.getString("username", null);
        today = LocalDate.now().toString();

        // API 호출
        api = RetrofitClient.getClient().create(TeacherApi.class);
        loadTodayClasses();
        ThemeColorUtil.applyThemeColor(this, toolbar);
    }

    private void loadTodayClasses() {
        Call<List<Course>> call = api.getClassesByTeacherIdAndDate(teacherId, today);
        call.enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Course> courses = response.body();
                    List<TeacherClass> teacherClasses = new ArrayList<>();
                    for (Course c : courses) {
                        teacherClasses.add(new TeacherClass(
                                c.getClassId(),
                                c.getClassName(),
                                c.getTeacherId(),
                                c.getSchedule()
                        ));
                    }

                    adapter = new TeacherClassAdapter(teacherClasses, TeacherTimetableActivity.this);
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(TeacherTimetableActivity.this, "수업 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                Toast.makeText(TeacherTimetableActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onClick(String classId) {
        // 수업 클릭 시: 출석 상세 보기 화면으로 이동
        Intent intent = new Intent(this, ClassAttendanceActivity.class);
        intent.putExtra("classId", classId);
        startActivity(intent);
    }
}
