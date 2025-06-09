package com.mobile.greenacademypartner.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.model.TeacherSignupRequest;

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
            long phone = Long.parseLong(editPhone.getText().toString().trim());
            int academy = Integer.parseInt(editAcademy.getText().toString().trim());

            TeacherSignupRequest request = new TeacherSignupRequest(name, id, pw, phone, academy);

            TeacherApi api = RetrofitClient.getClient().create(TeacherApi.class);
            api.signupTeacher(request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(TeacherSignupActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
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
