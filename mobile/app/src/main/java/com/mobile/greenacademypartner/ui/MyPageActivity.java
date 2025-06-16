package com.mobile.greenacademypartner.ui;

<<<<<<< HEAD
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
=======

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
>>>>>>> sub

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.mobile.greenacademypartner.R;
<<<<<<< HEAD
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.ParentUpdateRequest;
import com.mobile.greenacademypartner.model.StudentUpdateRequest;
import com.mobile.greenacademypartner.model.TeacherUpdateRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
=======
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
>>>>>>> sub

public class MyPageActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
<<<<<<< HEAD
    private LinearLayout navContainer;
    private Toolbar toolbar;
    private EditText editName, editId, editPhone, editAddress, editSchool, editGrade, editGender, editAcademyNumber;
    private TextView textRoleTitle;
    private Button btnSave;
    private String role;

    int defaultIndex = 0;
=======
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private TextView mainContentText;
>>>>>>> sub

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
<<<<<<< HEAD
        setContentView(R.layout.activity_my_page);

        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        toolbar = findViewById(R.id.toolbar);
=======
        setContentView(R.layout.activity_mypage);  // XML 연결

        drawerLayout = findViewById(R.id.drawer_layout_mypage);
        toolbar = findViewById(R.id.toolbar_mypage);
        navContainer = findViewById(R.id.nav_container_mypage);
        mainContentText = findViewById(R.id.main_content_text_mypage);

>>>>>>> sub
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

<<<<<<< HEAD
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, defaultIndex);

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

        String address = pref.getString("address", null);
        String school = pref.getString("school", null);
        int grade = pref.getInt("grade", 0);
        String gender = pref.getString("gender", null);

        role = pref.getString("role", "unknown").trim().toLowerCase();
        Log.d("MyPage", "address=" + pref.getString("address", "null"));
        Log.d("MyPage", "school=" + pref.getString("school", "null"));
        Log.d("MyPage", "grade=" + pref.getInt("grade", -1));
        Log.d("MyPage", "gender=" + pref.getString("gender", "null"));

        Log.d("MyPage", "전체 SharedPreferences 내용 확인:");
        Map<String, ?> allEntries = pref.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("MyPage", entry.getKey() + ": " + entry.getValue().toString());
        }

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

            // ✅ SharedPreferences 준비
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

                // 🟢 서버에 전송
                StudentUpdateRequest student = new StudentUpdateRequest(id, name, phone, address, school, grade, gender);
                StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
                api.updateStudent(id, student).enqueue(getCallback("학생"));

                // 🟢 SharedPreferences에 저장
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

            // ✅ 저장 완료
            editor.apply();
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
=======
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, mainContentText);
    }






>>>>>>> sub
}
