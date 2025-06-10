package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.mobile.greenacademypartner.model.ParentUpdateRequest;
import com.mobile.greenacademypartner.model.StudentUpdateRequest;
import com.mobile.greenacademypartner.model.TeacherUpdateRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyPageActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;

    private EditText editName, editId, editPhone, editAddress, editSchool, editGrade, editGender, editAcademyNumber;
    private TextView textRoleTitle, mainContentText;
    private Button btnSave;

    private String role;

    int defaultIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        // 1. 드로어 및 툴바
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navContainer = findViewById(R.id.nav_container);
        mainContentText = findViewById(R.id.main_content_text);
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, mainContentText, defaultIndex);

        // 2. 정보 입력 영역 초기화
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
        role = pref.getString("role", "unknown");
        String name = pref.getString("name", "");
        String id = pref.getString("username", "");
        String phone = pref.getString("phone", "");

        editName.setText(name);
        editId.setText(id);
        editPhone.setText(phone);

        switch (role) {
            case "student":
                editAddress.setText(pref.getString("address", ""));
                editSchool.setText(pref.getString("school", ""));
                editGrade.setText(String.valueOf(pref.getInt("grade", 0)));
                editGender.setText(pref.getString("gender", ""));
                break;
            case "teacher":
                editAcademyNumber.setText(String.valueOf(pref.getInt("academyNumber", 0)));
                break;
        }
    }

    private void setupUIByRole() {
        switch (role) {
            case "student":
                textRoleTitle.setText("학생 마이페이지");
                editAddress.setVisibility(View.VISIBLE);
                editSchool.setVisibility(View.VISIBLE);
                editGrade.setVisibility(View.VISIBLE);
                editGender.setVisibility(View.VISIBLE);
                break;
            case "teacher":
                textRoleTitle.setText("교사 마이페이지");
                editAcademyNumber.setVisibility(View.VISIBLE);
                break;
            case "parent":
                textRoleTitle.setText("학부모 마이페이지");
                break;
            default:
                textRoleTitle.setText("역할을 불러올 수 없습니다");
                break;
        }
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            String id = editId.getText().toString();
            String name = editName.getText().toString();
            String phone = editPhone.getText().toString();

            switch (role) {
                case "student": {
                    String address = editAddress.getText().toString();
                    String school = editSchool.getText().toString();
                    int grade = Integer.parseInt(editGrade.getText().toString());
                    String gender = editGender.getText().toString();

                    StudentUpdateRequest student = new StudentUpdateRequest(id, name, phone, address, school, grade, gender);
                    StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
                    api.updateStudent(id, student).enqueue(getCallback("학생"));
                    break;
                }
                case "parent": {
                    ParentUpdateRequest parent = new ParentUpdateRequest(id, name, phone);
                    ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
                    api.updateParent(id, parent).enqueue(getCallback("학부모"));
                    break;
                }
                case "teacher": {
                    int academyNumber = Integer.parseInt(editAcademyNumber.getText().toString());
                    TeacherUpdateRequest teacher = new TeacherUpdateRequest(id, name, phone, academyNumber);
                    TeacherApi api = RetrofitClient.getClient().create(TeacherApi.class);
                    api.updateTeacher(id, teacher).enqueue(getCallback("교사"));
                    break;
                }
            }
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
        if (success) {
            Toast.makeText(this, roleName + " 정보가 성공적으로 수정되었습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, roleName + " 정보 수정에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
