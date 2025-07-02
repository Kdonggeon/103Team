package com.mobile.greenacademypartner.ui.signup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.model.teacher.TeacherSignupRequest;
import com.mobile.greenacademypartner.ui.login.LoginActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeacherSignupActivity extends AppCompatActivity {

    private EditText editName, editId, editPw, editPhone, editAcademy;
    private Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_signup);

        editName = findViewById(R.id.edit_teacher_name);
        editId = findViewById(R.id.edit_teacher_id);
        editPw = findViewById(R.id.edit_teacher_pw);
        editPhone = findViewById(R.id.edit_teacher_phone);
        editAcademy = findViewById(R.id.edit_academy_number);
        btnSignup = findViewById(R.id.btn_teacher_signup);

        btnSignup.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String id = editId.getText().toString().trim();
            String pw = editPw.getText().toString().trim();
            String phoneStr = editPhone.getText().toString().trim();
            String academyStr = editAcademy.getText().toString().trim();

            // 입력값 유효성 검사
            if (name.isEmpty() || id.isEmpty() || pw.isEmpty() || phoneStr.isEmpty() || academyStr.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 전화번호 정규식 검사 (10~11자리 숫자)
            if (!phoneStr.matches("^\\d{10,11}$")) {
                Toast.makeText(this, "전화번호는 숫자만 입력하며, 10~11자리여야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            int academy;
            try {
                academy = Integer.parseInt(academyStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "학원번호는 숫자여야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            TeacherSignupRequest request = new TeacherSignupRequest(name, id, pw, phoneStr, academy);

            TeacherApi api = RetrofitClient.getClient().create(TeacherApi.class);
            api.signupTeacher(request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(TeacherSignupActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(TeacherSignupActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(TeacherSignupActivity.this, "회원가입 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(TeacherSignupActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
