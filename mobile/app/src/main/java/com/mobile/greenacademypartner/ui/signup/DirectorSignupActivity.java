package com.mobile.greenacademypartner.ui.signup;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.DirectorApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.director.DirectorSignupRequest;
import com.mobile.greenacademypartner.ui.login.LoginActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DirectorSignupActivity extends AppCompatActivity {

    private EditText editName, editId, editPw, editPwConfirm, editPhone, editAcademies;
    private Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_director_signup);

        initViews();
        setupListeners();
        ThemeColorUtil.applyThemeColor(this);
    }

    private void initViews() {
        editName       = findViewById(R.id.edit_director_name);
        editId         = findViewById(R.id.edit_director_id);
        editPw         = findViewById(R.id.edit_director_pw);
        editPwConfirm  = findViewById(R.id.edit_director_pw_confirm);
        editPhone      = findViewById(R.id.edit_director_phone);
        editAcademies  = findViewById(R.id.edit_director_academies); // 콤마로 구분된 숫자들
        btnSignup      = findViewById(R.id.btn_director_signup);
    }

    private void setupListeners() {
        btnSignup.setOnClickListener(v -> {
            String name = s(editName);
            String id = s(editId);
            String pw = s(editPw);
            String pwConfirm = s(editPwConfirm);
            String phone = s(editPhone);
            String academiesInput = s(editAcademies);

            // 기본 유효성
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(id) || TextUtils.isEmpty(pw)
                    || TextUtils.isEmpty(pwConfirm) || TextUtils.isEmpty(phone)) {
                toast("모든 항목을 입력하세요.");
                return;
            }

            if (!pw.equals(pwConfirm)) {
                toast("비밀번호가 일치하지 않습니다.");
                return;
            }

            if (!isValidPassword(pw)) {
                toast("비밀번호는 8자 이상이며, 문자, 숫자, 특수문자를 포함해야 합니다.");
                return;
            }

            if (!phone.matches("^\\d{10,11}$")) {
                toast("전화번호는 숫자만 입력하며, 10~11자리여야 합니다.");
                return;
            }

            // 학원번호 파싱 (예: "103, 105")
            List<Integer> academyNumbers = parseAcademyNumbers(academiesInput);
            if (academyNumbers.isEmpty()) {
                toast("학원 번호를 콤마(,)로 구분해 입력하세요. 예: 103,105");
                return;
            }

            DirectorSignupRequest request =
                    new DirectorSignupRequest(name, id, pw, phone, academyNumbers);

            DirectorApi api = RetrofitClient.getClient().create(DirectorApi.class);
            api.signup(request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        toast("회원가입 성공!");
                        Intent intent = new Intent(DirectorSignupActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        toast("회원가입 실패: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    toast("네트워크 오류: " + t.getMessage());
                }
            });
        });
    }

    private String s(EditText et) {
        return et.getText().toString().trim();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private boolean isValidPassword(String password) {
        String pattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
        return password.matches(pattern);
    }

    private List<Integer> parseAcademyNumbers(String input) {
        List<Integer> list = new ArrayList<>();
        if (TextUtils.isEmpty(input)) return list;

        String[] tokens = input.split(",");
        for (String t : tokens) {
            String trimmed = t.trim();
            if (trimmed.isEmpty()) continue;
            try {
                list.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException e) {
                // 숫자 아닌 값이 있으면 전체 실패 처리
                list.clear();
                break;
            }
        }
        return list;
    }
}
