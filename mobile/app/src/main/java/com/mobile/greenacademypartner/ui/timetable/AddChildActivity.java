package com.mobile.greenacademypartner.ui.timetable;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.parent.AddChildrenRequest;
import com.mobile.greenacademypartner.ui.attendance.AttendanceActivity;
import com.mobile.greenacademypartner.ui.main.MainActivity;
import com.mobile.greenacademypartner.ui.mypage.MyPageActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;
import com.mobile.greenacademypartner.ui.timetable.QRScannerActivity;
import com.mobile.greenacademypartner.ui.timetable.StudentTimetableActivity;

import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddChildActivity extends AppCompatActivity {

    private EditText editStudentId;
    private FloatingActionButton btnAddChild;
    private String parentId;
    private ParentApi api;

    private BottomNavigationView bottomNavigation;
    private ImageButton btnHideNav, btnShowNav;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ✅ 최신 XML (하단 네비 + FAB은 “오른쪽 하단”)
        setContentView(R.layout.activity_add_child);

        // ── Toolbar ──
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("자녀 추가");
        ThemeColorUtil.applyThemeColor(this, toolbar);

        // ── UI ──
        editStudentId   = findViewById(R.id.edit_student_id);
        btnAddChild     = findViewById(R.id.btn_add_child);
        bottomNavigation= findViewById(R.id.bottom_navigation);

        // 부모 ID 확인
        parentId = getIntent().getStringExtra("parentId");
        if (parentId == null || parentId.trim().isEmpty()) {
            Toast.makeText(this, "부모 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        api = RetrofitClient.getClient().create(ParentApi.class);

        // ── 등록 액션 공통 함수 ──
        View.OnClickListener submit = v -> {
            String studentId = editStudentId.getText().toString().trim();
            if (studentId.isEmpty()) {
                Toast.makeText(this, "학생 ID를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnAddChild.setEnabled(false); // 중복 클릭 방지
            AddChildrenRequest request = new AddChildrenRequest(Collections.singletonList(studentId));
            api.addChildren(parentId, request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    btnAddChild.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(AddChildActivity.this, "자녀가 추가되었습니다.", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(AddChildActivity.this,
                                "추가 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    btnAddChild.setEnabled(true);
                    Toast.makeText(AddChildActivity.this,
                            "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        };

        // FAB 클릭 → 서버 요청
        btnAddChild.setOnClickListener(submit);

        // 키보드 완료(IME_ACTION_DONE)로도 제출 가능
        editStudentId.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit.onClick(tv);
                return true;
            }
            return false;
        });

        // ── 하단 네비 이동 ──
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_attendance) {
                    startActivity(new Intent(this, AttendanceActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_qr) {
                    startActivity(new Intent(this, QRScannerActivity.class));
                    return true;
                } else if (id == R.id.nav_timetable) {
                    startActivity(new Intent(this, StudentTimetableActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_my) {
                    startActivity(new Intent(this, MyPageActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            });
        }
    }
}
