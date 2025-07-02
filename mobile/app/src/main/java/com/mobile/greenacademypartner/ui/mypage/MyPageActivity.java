package com.mobile.greenacademypartner.ui.mypage;

import android.content.SharedPreferences;
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
import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.parent.ParentUpdateRequest;
import com.mobile.greenacademypartner.model.student.StudentUpdateRequest;
import com.mobile.greenacademypartner.model.teacher.TeacherUpdateRequest;

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
        loadUserInfo();
        setupUIByRole();
        setupSaveButton();
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

    private void loadUserInfo() {
        SharedPreferences pref = getSharedPreferences("login_prefs", MODE_PRIVATE);

        role = pref.getString("role", "unknown").trim().toLowerCase();

        editName.setText(pref.getString("name", ""));
        editId.setText(pref.getString("username", ""));
        editPhone.setText(pref.getString("phone", ""));

        if ("student".equals(role)) {
            editAddress.setText(pref.getString("address", ""));
            editSchool.setText(pref.getString("school", ""));
            editGrade.setText(String.valueOf(pref.getInt("grade", 0)));
            editGender.setText(pref.getString("gender", ""));
        } else if ("teacher".equals(role)) {
            editAcademyNumber.setText(String.valueOf(pref.getInt("academyNumber", 0)));
        }

        // SharedPreferences 전체 로그
        Map<String, ?> allEntries = pref.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("MyPage", entry.getKey() + ": " + entry.getValue());
        }
    }

    private void setupUIByRole() {
        if ("student".equals(role)) {
            textRoleTitle.setText("학생 마이페이지");
            editAddress.setVisibility(View.VISIBLE);
            editSchool.setVisibility(View.VISIBLE);
            editGrade.setVisibility(View.VISIBLE);
            editGender.setVisibility(View.VISIBLE);
        } else if ("teacher".equals(role)) {
            textRoleTitle.setText("교사 마이페이지");
            editAcademyNumber.setVisibility(View.VISIBLE);
        } else if ("parent".equals(role)) {
            textRoleTitle.setText("학부모 마이페이지");
        } else {
            textRoleTitle.setText("역할을 불러올 수 없습니다");
        }
    }

    private void setupSaveButton() {
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
                String address = editAddress.getText().toString();
                String school = editSchool.getText().toString();
                int grade = Integer.parseInt(editGrade.getText().toString());
                String gender = editGender.getText().toString();

                StudentUpdateRequest student = new StudentUpdateRequest(id, name, phone, address, school, grade, gender);
                StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
                api.updateStudent(id, student).enqueue(getCallback("학생"));

                editor.putString("address", address);
                editor.putString("school", school);
                editor.putInt("grade", grade);
                editor.putString("gender", gender);

            } else if ("parent".equals(role)) {
                ParentUpdateRequest parent = new ParentUpdateRequest(id, name, phone);
                ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
                api.updateParent(id, parent).enqueue(getCallback("학부모"));

            } else if ("teacher".equals(role)) {
                int academyNumber = Integer.parseInt(editAcademyNumber.getText().toString());
                TeacherUpdateRequest teacher = new TeacherUpdateRequest(id, name, phone, academyNumber);
                TeacherApi api = RetrofitClient.getClient().create(TeacherApi.class);
                api.updateTeacher(id, teacher).enqueue(getCallback("교사"));

                editor.putInt("academyNumber", academyNumber);
            }

            editor.apply(); // 저장
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
}
