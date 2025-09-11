package com.mobile.greenacademypartner.ui.mypage;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.parent.ParentUpdateRequest;
import com.mobile.greenacademypartner.model.student.StudentUpdateRequest;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyPageActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private Toolbar toolbar;
    private EditText editName, editId, editPhone, editAddress, editSchool, editGrade, editGender, editAcademyNumber;
    private TextView textRoleTitle;
    private Button btnSave;
    private String role;

    private final int defaultIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        // Drawer & Toolbar 설정
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        toolbar = findViewById(R.id.toolbar);
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, defaultIndex);

        // 주요 구성요소 초기화
        initViews();

        // 사용자 정보 로드 및 제목 반영
        loadUserInfoAndSetTitles();

        // 역할별 UI 표시/숨김 (+ 학생은 전부 읽기전용 처리)
        setupUIByRole();

        // 저장 버튼 클릭
        setupSaveButton();

        ThemeColorUtil.applyThemeColor(this, toolbar);
    }

    private void initViews() {
        textRoleTitle = findViewById(R.id.text_role_title);
        editName = findViewById(R.id.edit_name);
        editId = findViewById(R.id.edit_id);
        editPhone = findViewById(R.id.edit_phone);
        editAddress = findViewById(R.id.edit_address);
        editSchool = findViewById(R.id.edit_school);
        editGrade = findViewById(R.id.edit_grade);
        editGender = findViewById(R.id.edit_gender);
        editAcademyNumber = findViewById(R.id.edit_academy_number);
        btnSave = findViewById(R.id.btn_save);
    }

    /**
     * SharedPreferences에서 사용자 정보를 읽고,
     * 툴바/헤더에 "OOO의 마이페이지"로 제목을 반영합니다.
     */
    private void loadUserInfoAndSetTitles() {
        SharedPreferences pref = getSharedPreferences("login_prefs", MODE_PRIVATE);

        role = pref.getString("role", "unknown").trim().toLowerCase();

        String name = pref.getString("name", "");
        String username = pref.getString("username", "");
        String phone = pref.getString("phone", "");

        editName.setText(name);
        editId.setText(username);
        editPhone.setText(phone);

        if ("student".equals(role)) {
            editAddress.setText(pref.getString("address", ""));
            editSchool.setText(pref.getString("school", ""));
            int grade = pref.getInt("grade", 0);
            editGrade.setText(grade == 0 ? "" : String.valueOf(grade));
            editGender.setText(pref.getString("gender", ""));
        }

        // 제목 구성
        String titleName = (name != null && !name.trim().isEmpty()) ? name.trim() : "사용자";
        String fullTitle = titleName + "의 마이페이지";

        // 툴바/헤더 적용
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fullTitle);
        }
        textRoleTitle.setText(fullTitle);

        // 전체 프리퍼런스 로그(디버그)
        Map<String, ?> allEntries = pref.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("MyPage", entry.getKey() + ": " + entry.getValue());
        }
    }

    /**
     * 역할별로 보일 입력 필드만 표시/숨김합니다.
     * 학생은 모든 필드를 "ID처럼" 편집 불가로 만들고 저장 버튼도 비활성화/숨김합니다.
     */
    private void setupUIByRole() {
        // 모두 숨김으로 초기화
        editAddress.setVisibility(View.GONE);
        editSchool.setVisibility(View.GONE);
        editGrade.setVisibility(View.GONE);
        editGender.setVisibility(View.GONE);
        editAcademyNumber.setVisibility(View.GONE);

        if ("student".equals(role)) {
            // 표시만 하고, 아래에서 전부 읽기전용으로 잠금
            editAddress.setVisibility(View.VISIBLE);
            editSchool.setVisibility(View.VISIBLE);
            editGrade.setVisibility(View.VISIBLE);
            editGender.setVisibility(View.VISIBLE);

            lockStudentFields(); // ★ 학생은 모든 필드를 ID처럼 편집 불가로

        } else if ("teacher".equals(role)) {
            editAcademyNumber.setVisibility(View.VISIBLE);
        } else if ("parent".equals(role)) {
            // 추가 표시 없음
        } else {
            // unknown
        }
    }

    private void setupSaveButton() {
        // 학생은 저장 자체를 막음(방어 로직)
        if ("student".equals(role)) {
            if (btnSave != null) {
                btnSave.setOnClickListener(v ->
                        Toast.makeText(this, "학생 정보는 수정할 수 없습니다.", Toast.LENGTH_SHORT).show()
                );
            }
            return;
        }

        btnSave.setOnClickListener(v -> {
            String id = editId.getText().toString();
            String name = editName.getText().toString();
            String phone = editPhone.getText().toString();

            SharedPreferences pref = getSharedPreferences("login_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();

            editor.putString("username", id);
            editor.putString("name", name);
            editor.putString("phone", phone);

            if ("student".equals(role)) {
                // 학생은 여기 오지 않지만, 혹시 몰라 이중 방어
                Toast.makeText(this, "학생 정보는 수정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            } else if ("parent".equals(role)) {
                ParentUpdateRequest parent = new ParentUpdateRequest(id, name, phone);
                ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
                api.updateParent(id, parent).enqueue(getCallback("학부모"));
            }
            // 교사 저장 로직 필요 시 여기에 추가

            editor.apply();

            // 저장 후 제목 갱신(이름 바뀐 경우 반영)
            loadUserInfoAndSetTitles();
        });
    }

    private Callback<Void> getCallback(String roleName) {
        return new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                showToast(response.isSuccessful(), roleName);
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showToast(false, roleName);
            }
        };
    }

    private void showToast(boolean success, String roleName) {
        String msg = roleName + (success ? " 정보가 성공적으로 수정되었습니다." : " 정보 수정에 실패했습니다.");
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /** 학생: 모든 필드를 "ID처럼" 읽기전용으로 만들고 저장 버튼 비활성화/숨김 */
    private void lockStudentFields() {
        makeReadOnly(editId);       // 혹시 모를 활성화 대비
        makeReadOnly(editName);
        makeReadOnly(editPhone);
        makeReadOnly(editAddress);
        makeReadOnly(editSchool);
        makeReadOnly(editGrade);
        makeReadOnly(editGender);

                tintGray(editId);
                tintGray(editName);
                tintGray(editPhone);
               tintGray(editAddress);
                tintGray(editSchool);
                tintGray(editGrade);
                tintGray(editGender);

        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setVisibility(View.GONE);
        }
    }

    /** 외형은 유지하면서 입력만 차단(복사 허용) */
    private void makeReadOnly(EditText et) {
        if (et == null) return;
        et.setFocusable(false);
        et.setFocusableInTouchMode(false);
        et.setClickable(false);
        et.setLongClickable(false);
        et.setCursorVisible(false);
        et.setTextIsSelectable(true); // 복사 허용
        et.setKeyListener(null);      // 핵심: 키 입력 차단
    }
        /** 텍스트만 회색으로 */
                private void tintGray(EditText et) {
                if (et == null) return;
                et.setTextColor(Color.parseColor("#6B7280"));  // 적당한 중간 회색
                et.setHintTextColor(Color.parseColor("#9AA0A6")); // 힌트는 더 옅게
            }
}

