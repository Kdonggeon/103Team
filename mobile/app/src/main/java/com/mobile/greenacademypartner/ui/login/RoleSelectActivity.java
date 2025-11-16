package com.mobile.greenacademypartner.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;
import com.mobile.greenacademypartner.ui.signup.ParentSignupActivity;
import com.mobile.greenacademypartner.ui.signup.StudentSignupActivity;
//import com.mobile.greenacademypartner.ui.signup.TeacherSignupActivity;
//import com.mobile.greenacademypartner.ui.signup.DirectorSignupActivity; // ✅ 추가

public class RoleSelectActivity extends AppCompatActivity {

    private Button btnStudent, btnParent, btnTeacher, btnDirector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_select);

        // ✅ 테마 색 적용 (모든 버튼 자동 색 변경)
        ThemeColorUtil.applyThemeColor(this);

        btnStudent = findViewById(R.id.btn_student);
        btnParent = findViewById(R.id.btn_parent);
//        btnTeacher = findViewById(R.id.btn_teacher);
//        btnDirector = findViewById(R.id.btn_director);

        btnStudent.setOnClickListener(v -> {
            startActivity(new Intent(this, StudentSignupActivity.class));
        });

        btnParent.setOnClickListener(v -> {
            startActivity(new Intent(this, ParentSignupActivity.class));
        });

//        btnTeacher.setOnClickListener(v -> {
//            startActivity(new Intent(this, TeacherSignupActivity.class));
//        });
//
//        btnDirector.setOnClickListener(v -> {
//            startActivity(new Intent(this, DirectorSignupActivity.class));
//        });
    }
}
