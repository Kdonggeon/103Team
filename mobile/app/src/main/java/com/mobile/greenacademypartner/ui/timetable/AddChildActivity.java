package com.mobile.greenacademypartner.ui.timetable;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.parent.AddChildrenRequest;

import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddChildActivity extends AppCompatActivity {

    private EditText editStudentId;
    private FloatingActionButton btnAddChild;
    private String parentId;
    private ParentApi api;

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_child);

        // 툴바 설정
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("자녀 조회");

        // 드로어 설정
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 사이드 메뉴 설정
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, 2);


        // UI 요소 연결
        editStudentId = findViewById(R.id.edit_student_id);
        btnAddChild = findViewById(R.id.btn_add_child);

        // parentId 전달받기
        parentId = getIntent().getStringExtra("parentId");
        if (parentId == null) {
            Toast.makeText(this, "부모 정보가 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        api = RetrofitClient.getClient().create(ParentApi.class);

        btnAddChild.setOnClickListener(v -> {
            String studentId = editStudentId.getText().toString().trim();
            if (studentId.isEmpty()) {
                Toast.makeText(this, "학생 ID를 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 서버 요청
            AddChildrenRequest request = new AddChildrenRequest(Collections.singletonList(studentId));
            api.addChildren(parentId, request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddChildActivity.this, "자녀가 추가되었습니다", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(AddChildActivity.this, "자녀 추가 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(AddChildActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
