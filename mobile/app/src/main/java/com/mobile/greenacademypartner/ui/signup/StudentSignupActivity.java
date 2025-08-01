package com.mobile.greenacademypartner.ui.signup;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.model.student.StudentSignupRequest;
import com.mobile.greenacademypartner.model.student.Student;
import com.mobile.greenacademypartner.ui.login.LoginActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.io.IOException;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentSignupActivity extends AppCompatActivity {

    private EditText editId, editPw, editPwConfirm, editName, editPhone, editBirth;
    private RadioGroup radioGender;
    private Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_signup);

        initViews();
        setupListeners();
        ThemeColorUtil.applyThemeColor(this);
    }

    private void initViews() {
        editId = findViewById(R.id.edit_id);
        editPw = findViewById(R.id.edit_pw);
        editPwConfirm = findViewById(R.id.edit_pw_confirm);
        editName = findViewById(R.id.edit_name);
        editPhone = findViewById(R.id.edit_phone);
        editBirth = findViewById(R.id.edit_birth);
        radioGender = findViewById(R.id.radio_gender);
        btnSignup = findViewById(R.id.btn_signup);

        editPhone.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
    }

    private void setupListeners() {
        editBirth.setOnClickListener(v -> showDatePicker());

        btnSignup.setOnClickListener(v -> {
            String idStr = editId.getText().toString().trim();
            String pwStr = editPw.getText().toString().trim();
            String pwConfirmStr = editPwConfirm.getText().toString().trim();
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String birth = editBirth.getText().toString().trim();
            String gender = (radioGender.getCheckedRadioButtonId() == R.id.radio_male) ? "남성" : "여성";

            Log.d("회원가입 입력값", "ID: " + idStr + ", PW: " + pwStr + ", Name: " + name);

            if (idStr.isEmpty() || pwStr.isEmpty() || pwConfirmStr.isEmpty() ||
                    name.isEmpty() || phone.isEmpty() || birth.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pwStr.equals(pwConfirmStr)) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPassword(pwStr)) {
                Toast.makeText(this, "비밀번호는 8자 이상이며, 문자, 숫자, 특수문자를 포함해야 합니다.", Toast.LENGTH_LONG).show();
                return;
            }

            if (pwStr == null || pwStr.trim().isEmpty()) {
                Log.e("회원가입 오류", "비밀번호가 null 또는 빈 문자열입니다");
                Toast.makeText(this, "비밀번호를 다시 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            StudentSignupRequest request = new StudentSignupRequest(
                    idStr,                  // Student_ID
                    pwStr,                  // Student_PW
                    name,                   // Student_Name
                    "서울시 강남구",          // Student_Address (입력값 또는 임시값)
                    phone,                  // Student_Phone_Number
                    "서울고등학교",          // School (입력값 또는 임시값)
                    3,                      // Grade (입력값 또는 기본값)
                    "1001",                  // Parents_Number (임시 또는 실제 값)
                    12,                     // Seat_Number (좌석번호 - 기본값 가능)
                    false,                 // Checked_In (처음엔 false 고정)
                    gender                  // Gender
            );

            Log.d("회원가입 요청", "PW: " + request.getStudentPw()); // ← 서버 전송 전 확인

            StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
            api.signupStudent(request).enqueue(new Callback<Student>() {
                @Override
                public void onResponse(Call<Student> call, Response<Student> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Student savedStudent = response.body();
                        Toast.makeText(StudentSignupActivity.this,
                                "회원가입 성공! " + savedStudent.getStudentName(), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(StudentSignupActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        String errorMsg = "서버 오류";
                        try {
                            if (response.errorBody() != null) {
                                errorMsg = response.errorBody().string();
                            }
                        } catch (IOException e) {
                            errorMsg = "에러 본문 파싱 실패: " + e.getMessage();
                        }

                        int statusCode = response.code();
                        Log.e("회원가입 오류", "code: " + statusCode + ", body: " + errorMsg);
                        Toast.makeText(StudentSignupActivity.this,
                                "서버 오류 (" + statusCode + "): " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Student> call, Throwable t) {
                    Toast.makeText(StudentSignupActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("회원가입 네트워크 오류", t.getMessage(), t);
                }
            });
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String birthStr = String.format("%04d-%02d-%02d", year, month + 1, day);
            editBirth.setText(birthStr);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private boolean isValidPassword(String password) {
        String pattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
        return password.matches(pattern);
    }
}
