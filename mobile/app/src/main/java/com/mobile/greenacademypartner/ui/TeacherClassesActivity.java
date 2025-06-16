package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.Course;
import com.mobile.greenacademypartner.ui.adapter.CourseAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeacherClassesActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private Toolbar toolbar;
    private ListView listView;
    private String classId;
    private String teacherId;
    private CourseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_attendance);

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        listView = findViewById(R.id.attendance_list);

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

        // ✅ SharedPreferences에서 teacherId 불러오기
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        teacherId = prefs.getString("username", null); // username == teacherId
        Log.d("TeacherClasses", "teacherId: " + teacherId);

        if (teacherId == null) {
            Toast.makeText(this, "교사 ID를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish(); // 더 진행하지 않음
            return;
        }

        classId = getIntent().getStringExtra("classId"); // 필요 시 사용
        loadCourses(); // 반드시 호출
    }

    private void loadCourses() {
        TeacherApi api = RetrofitClient.getClient().create(TeacherApi.class);
        api.getCoursesByTeacherId(teacherId).enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter = new CourseAdapter(TeacherClassesActivity.this, response.body());
                    listView.setAdapter(adapter);

                    listView.setOnItemClickListener((parent, view, position, id) -> {
                        Course selected = response.body().get(position);
                        Intent intent = new Intent(TeacherClassesActivity.this, ClassAttendanceActivity.class);
                        intent.putExtra("classId", selected.getClassId());
                        startActivity(intent);
                    });
                } else {
                    Toast.makeText(TeacherClassesActivity.this, "수업 조회 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                Toast.makeText(TeacherClassesActivity.this, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("TeacherClasses", "수업 조회 오류: ", t);
            }
        });
    }
}
