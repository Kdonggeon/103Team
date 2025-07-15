package com.mobile.greenacademypartner.ui.signup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.parent.ParentSignupRequest;
import com.mobile.greenacademypartner.ui.login.LoginActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

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



        initViews();
        setupListeners();
        ThemeColorUtil.applyThemeColor(this);
    }

    private void initViews() {
        editName = findViewById(R.id.edit_parent_name);
        editId = findViewById(R.id.edit_parent_id);
        editPw = findViewById(R.id.edit_parent_pw);
        editPwConfirm = findViewById(R.id.edit_parent_pw_confirm);
        editPhone = findViewById(R.id.edit_parent_phone);
        btnSignup = findViewById(R.id.btn_parent_signup);
    }

    private void setupListeners() {
        btnSignup.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String id = editId.getText().toString().trim();
            String pw = editPw.getText().toString().trim();
            String pwConfirm = editPwConfirm.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();

            // 입력값 유효성 검사
            if (id.isEmpty() || pw.isEmpty() || pwConfirm.isEmpty() || name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pw.equals(pwConfirm)) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPassword(pw)) {
                Toast.makeText(this, "비밀번호는 8자 이상이며, 문자, 숫자, 특수문자를 포함해야 합니다.", Toast.LENGTH_LONG).show();
                return;
            }

            if (!phone.matches("^\\d{10,11}$")) {
                Toast.makeText(this, "전화번호는 숫자만 입력하며, 10~11자리여야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 요청 객체 생성
            ParentSignupRequest request = new ParentSignupRequest(id, pw, name, phone);

            // Retrofit API 호출
            ParentApi api = RetrofitClient.getClient().create(ParentApi.class);

            api.signupParent(request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(ParentSignupActivity.this, "회원가입 성공!", Toast.LENGTH_SHORT).show();
                        // 로그인 화면으로 이동
                        Intent intent = new Intent(ParentSignupActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
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

    private boolean isValidPassword(String password) {
        String pattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
        return password.matches(pattern);
    }
}
