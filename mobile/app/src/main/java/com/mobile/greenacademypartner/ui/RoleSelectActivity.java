package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;

public class RoleSelectActivity extends AppCompatActivity {

    private Button btnStudent, btnParent, btnTeacher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_select);

        btnStudent = findViewById(R.id.btn_student);
        btnParent = findViewById(R.id.btn_parent);
        btnTeacher = findViewById(R.id.btn_teacher);

        btnStudent.setOnClickListener(v -> {
            startActivity(new Intent(this, StudentSignupActivity.class));
        });

        btnParent.setOnClickListener(v -> {
            startActivity(new Intent(this, ParentSignupActivity.class));
        });

        btnTeacher.setOnClickListener(v -> {
            startActivity(new Intent(this, TeacherSignupActivity.class));
        });
    }
}
