package com.mobile.greenacademypartner.ui.login;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.PasswordResetApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.login.PasswordResetRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText editId, editName, editPhone, editNewPw;
    private RadioGroup radioRole;
    private Button btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        editId = findViewById(R.id.edit_id);
        editName = findViewById(R.id.edit_name);
        editPhone = findViewById(R.id.edit_phone);
        editNewPw = findViewById(R.id.edit_new_pw);
        radioRole = findViewById(R.id.radio_role);
        btnReset = findViewById(R.id.btn_reset);

        btnReset.setOnClickListener(v -> {
            String id = editId.getText().toString().trim();
            String name = editName.getText().toString().trim();
            String phoneStr = editPhone.getText().toString().trim();
            String newPw = editNewPw.getText().toString().trim();

            if (id.isEmpty() || name.isEmpty() || phoneStr.isEmpty() || newPw.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            String role = getSelectedRole();
            if (role == null) {
                Toast.makeText(this, "역할을 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            String phone;
            try {
                phone = phoneStr.replaceAll("[^\\d]", ""); // 숫자만 추출
            } catch (Exception e) {
                Toast.makeText(this, "전화번호 형식이 올바르지 않습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            PasswordResetRequest request = new PasswordResetRequest(role, id, name, phone, newPw);

            PasswordResetApi api = RetrofitClient.getClient().create(PasswordResetApi.class);

            api.resetPassword(request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(ResetPasswordActivity.this, "비밀번호 재설정 성공", Toast.LENGTH_SHORT).show();
                        finish(); // 로그인 화면으로 이동
                    } else {
                        Toast.makeText(ResetPasswordActivity.this, "실패: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(ResetPasswordActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String getSelectedRole() {
        int id = radioRole.getCheckedRadioButtonId();
        if (id == R.id.radio_student) return "student";
        if (id == R.id.radio_parent) return "parent";
        if (id == R.id.radio_teacher) return "teacher";
        return null;
    }
}
