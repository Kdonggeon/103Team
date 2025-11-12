package com.example.qr.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qr.R;
import com.example.qr.api.AuthApi;
import com.example.qr.api.RetrofitClient;
import com.example.qr.model.login.LoginRequest;
import com.example.qr.model.login.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText edtId, edtPw, edtAcademyNum;
    private Button btnLogin;
    private ImageView btnTogglePw;
    private CheckBox chkAutoLogin;

    private SharedPreferences prefs;
    private static final String PREF_NAME = "academy_login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_academy_login);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        checkAutoLogin();

        edtId = findViewById(R.id.edit_academy_id);
        edtPw = findViewById(R.id.edit_academy_pw);
        edtAcademyNum = findViewById(R.id.edit_academy_number);
        btnLogin = findViewById(R.id.btn_academy_login);
        btnTogglePw = findViewById(R.id.btn_toggle_pw);
        chkAutoLogin = findViewById(R.id.chk_auto_login);

        btnTogglePw.setOnClickListener(v -> togglePassword());
        btnLogin.setOnClickListener(v -> loginDirector());
    }

    private void checkAutoLogin() {
        boolean saved = prefs.getBoolean("autoLogin", false);
        String savedAcademyNumber = prefs.getString("academyNumber", null);
        String savedToken = prefs.getString("director_token", null);

        if (saved && savedToken != null && savedAcademyNumber != null) {
            Intent intent = new Intent(this, QrLoginTabletActivity.class);
            intent.putExtra("academyNumber", savedAcademyNumber);
            startActivity(intent);
            finish();
        }
    }

    private void togglePassword() {
        int inputType = edtPw.getInputType();
        if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            edtPw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePw.setImageResource(R.drawable.eye);
        } else {
            edtPw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePw.setImageResource(R.drawable.eye_off);
        }
        edtPw.setSelection(edtPw.getText().length());
    }

    /** ✅ 원장 로그인 처리 (/api/login) */
    private void loginDirector() {
        String id = edtId.getText().toString().trim();
        String pw = edtPw.getText().toString().trim();
        String academyNumber = edtAcademyNum.getText().toString().trim();

        if (id.isEmpty() || pw.isEmpty() || academyNumber.isEmpty()) {
            Toast.makeText(this, "모든 값을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthApi api = RetrofitClient.getClient().create(AuthApi.class);
        LoginRequest request = new LoginRequest(id, pw);

        api.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(LoginActivity.this, "로그인 실패: 아이디 또는 비밀번호 확인", Toast.LENGTH_SHORT).show();
                    return;
                }

                LoginResponse res = response.body();
                if (!"director".equals(res.getRole())) {
                    Toast.makeText(LoginActivity.this, "원장 계정으로 로그인하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // ✅ JWT 토큰 저장 (QR 화면에서 사용)
                prefs.edit()
                        .putString("director_token", res.getToken())
                        .apply();

                // ✅ 자동 로그인 설정
                if (chkAutoLogin.isChecked()) {
                    prefs.edit()
                            .putBoolean("autoLogin", true)
                            .putString("academyNumber", academyNumber)
                            .apply();
                } else {
                    prefs.edit()
                            .putBoolean("autoLogin", false)
                            .remove("academyNumber")
                            .apply();
                }
                // ✅ 로그인 성공 알림 추가
                Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();

                // ✅ QR 화면으로 이동
                Intent intent = new Intent(LoginActivity.this, QrLoginTabletActivity.class);
                intent.putExtra("academyNumber", academyNumber);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "서버 연결 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
