package com.mobile.greenacademypartner.ui.classes;

import android.content.Intent;
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
import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.teacher.TeacherClass;
import com.mobile.greenacademypartner.ui.adapter.TeacherClassAdapter;
import com.mobile.greenacademypartner.ui.attendance.ClassAttendanceActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeacherClassesActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private Toolbar toolbar;
    private RecyclerView classListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_classes);

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        classListView = findViewById(R.id.class_list_view);

        setSupportActionBar(toolbar);
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setTitle("수업 목록");

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, 1);

        loadTeacherClasses();
        ThemeColorUtil.applyThemeColor(this, toolbar);
    }

    private void loadTeacherClasses() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String teacherId = prefs.getString("username", null); // SharedPreferences에 저장된 교사 ID 사용

        if (teacherId == null) {
            Toast.makeText(this, "로그인 정보 없음", Toast.LENGTH_SHORT).show();
            return;
        }

        TeacherApi api = RetrofitClient.getClient().create(TeacherApi.class);
        api.getClassesForTeacher(teacherId).enqueue(new Callback<List<TeacherClass>>() {
            @Override
            public void onResponse(Call<List<TeacherClass>> call, Response<List<TeacherClass>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TeacherClass> classes = response.body();

                    Log.d("수업목록 수", String.valueOf(classes.size()));  // ✅ 수업 개수 확인

                    if (classes.isEmpty()) {
                        Log.w("수업목록", "비어있음");
                    }
                    classListView.setLayoutManager(new LinearLayoutManager(TeacherClassesActivity.this));
                    TeacherClassAdapter adapter = new TeacherClassAdapter(classes, classId -> {
                        // 클릭 시 해당 수업의 출석 내역으로 이동
                        Intent intent = new Intent(TeacherClassesActivity.this, ClassAttendanceActivity.class);
                        intent.putExtra("classId", classId);
                        startActivity(intent);
                    });
                    classListView.setAdapter(adapter);
                } else {
                    Toast.makeText(TeacherClassesActivity.this, "수업 목록 불러오기 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<TeacherClass>> call, Throwable t) {
                Toast.makeText(TeacherClassesActivity.this, "서버 오류 발생", Toast.LENGTH_SHORT).show();
                Log.e("TeacherClasses", "API 실패", t);
            }
        });
    }
}
