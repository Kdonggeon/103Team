package com.mobile.greenacademypartner.ui;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.ParentSignupRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParentSignupActivity extends AppCompatActivity {

    private EditText editName, editId, editPw, editPwConfirm, editPhone;
    private Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_signup);

        editName = findViewById(R.id.edit_parent_name);
        editId = findViewById(R.id.edit_parent_id);
        editPw = findViewById(R.id.edit_parent_pw);
        editPwConfirm = findViewById(R.id.edit_parent_pw_confirm);
        editPhone = findViewById(R.id.edit_parent_phone);
        btnSignup = findViewById(R.id.btn_parent_signup);

        btnSignup.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String id = editId.getText().toString().trim();
            String pw = editPw.getText().toString().trim();
            String pwConfirm = editPwConfirm.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();

            if (id.isEmpty() || pw.isEmpty() || name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pw.equals(pwConfirm)) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            long phoneNumber;
            try {
                phoneNumber = Long.parseLong(phone.replaceAll("[^\\d]", ""));
            } catch (Exception e) {
                Toast.makeText(this, "전화번호 형식이 올바르지 않습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            ParentSignupRequest request = new ParentSignupRequest(id, pw, name, phoneNumber);
            ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
            api.signupParent(request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(ParentSignupActivity.this, "회원가입 성공!", Toast.LENGTH_SHORT).show();
                        finish(); // or 이동
                    } else {
                        Toast.makeText(ParentSignupActivity.this, "회원가입 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(ParentSignupActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
