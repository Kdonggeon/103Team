package com.example.qr.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qr.R;
import com.example.qr.api.AuthApi;
import com.example.qr.api.AttendanceApi;
import com.example.qr.api.AcademyApi;
import com.example.qr.api.RetrofitClient;
import com.example.qr.model.login.LoginRequest;
import com.example.qr.model.login.LoginResponse;
import com.example.qr.model.Student.Student;
import com.example.qr.ui.WaitingRoomActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QrLoginTabletActivity extends AppCompatActivity {

    private EditText editId, editPw;
    private Button btnLogin, btnLogout;
    private ImageView qrImage;

    private AuthApi authApi;
    private AttendanceApi attendanceApi;
    private AcademyApi academyApi;

    private SharedPreferences prefs;
    private String academyNumberFromIntent = "0000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_login_tablet);

        editId = findViewById(R.id.edit_id);
        editPw = findViewById(R.id.edit_pw);
        btnLogin = findViewById(R.id.btn_academy_login);
        btnLogout = findViewById(R.id.btn_logout);
        qrImage = findViewById(R.id.img_academy_qr);

        prefs = getSharedPreferences("academy_login", MODE_PRIVATE);

        authApi = RetrofitClient.getClient().create(AuthApi.class);
        attendanceApi = RetrofitClient.getClient().create(AttendanceApi.class);
        academyApi = RetrofitClient.getClient().create(AcademyApi.class);

        // ✅ 인텐트에서 학원번호 가져오기
        String fromIntent = getIntent().getStringExtra("academyNumber");
        if (fromIntent != null) academyNumberFromIntent = fromIntent;

        // ✅ 원장 토큰이 있으면 바로 QR 표시
        String directorToken = prefs.getString("director_token", null);
        if (directorToken != null) {
            loadStudentsAndMakeQr(academyNumberFromIntent, directorToken);
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnLogout.setOnClickListener(v -> logout());
    }

    /** ✅ 로그인 시도 */
    private void attemptLogin() {
        String id = editId.getText().toString().trim();
        String pw = editPw.getText().toString().trim();

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(pw)) {
            Toast.makeText(this, "아이디와 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        LoginRequest req = new LoginRequest(id, pw);

        authApi.login(req).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> res) {
                if (!res.isSuccessful() || res.body() == null) {
                    Toast.makeText(QrLoginTabletActivity.this, "아이디 또는 비밀번호가 잘못되었습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                LoginResponse data = res.body();

                // ✅ 원장 로그인 → QR 표시
                if ("director".equals(data.getRole())) {
                    prefs.edit().putString("director_token", data.getToken()).apply();
                    Toast.makeText(QrLoginTabletActivity.this, "원장 로그인 완료", Toast.LENGTH_SHORT).show();

                    if (data.getAcademyNumbers() != null && !data.getAcademyNumbers().isEmpty()) {
                        loadStudentsAndMakeQr(String.valueOf(data.getAcademyNumbers().get(0)), data.getToken());
                    }
                    return;
                }

                // ✅ 학생 로그인 → 출석 처리
                if ("student".equals(data.getRole())) {
                    // 🔥 추가: login_prefs에도 저장 (QR 스캐너에서 인식 가능하도록)
                    SharedPreferences loginPrefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
                    loginPrefs.edit()
                            .putString("student_id", data.getUsername())
                            .putString("token", data.getToken())
                            .apply();

                    checkIfStudentRegistered(data);
                } else {
                    Toast.makeText(QrLoginTabletActivity.this, "학생 계정으로 로그인하세요.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(QrLoginTabletActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
            }
        });

        hideKeyboard();
    }

    /** ✅ 학원 등록 여부 확인 */
    private void checkIfStudentRegistered(LoginResponse data) {
        String token = data.getToken();
        String studentId = data.getUsername();

        academyApi.getStudentsByAcademy("Bearer " + token, academyNumberFromIntent)
                .enqueue(new Callback<List<Student>>() {
                    @Override
                    public void onResponse(Call<List<Student>> call, Response<List<Student>> resp) {
                        if (!resp.isSuccessful() || resp.body() == null) {
                            Toast.makeText(QrLoginTabletActivity.this, "학생 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean registered = false;
                        for (Student s : resp.body()) {
                            if (s.getStudentId().equals(studentId)) {
                                registered = true;
                                break;
                            }
                        }

                        if (registered) {
                            doCheckIn(studentId, token);
                        } else {
                            Toast.makeText(QrLoginTabletActivity.this, "이 학원에 등록되지 않은 학생입니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Student>> call, Throwable t) {
                        Toast.makeText(QrLoginTabletActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** ✅ 출석 체크 (classId 없음) */
    private void doCheckIn(String studentId, String token) {
        Map<String, String> req = new HashMap<>();
        req.put("studentId", studentId);
        req.put("academyNumber", academyNumberFromIntent);

        attendanceApi.checkIn("Bearer " + token, req)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> res) {
                        if (res.isSuccessful()) {
                            Toast.makeText(QrLoginTabletActivity.this, "✅ 출석 완료! 대기실로 이동합니다.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(QrLoginTabletActivity.this, WaitingRoomActivity.class);
                            intent.putExtra("studentId", studentId);
                            intent.putExtra("academyNumber", academyNumberFromIntent);
                            startActivity(intent);
                        } else {
                            Toast.makeText(QrLoginTabletActivity.this, "출석 실패: " + res.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(QrLoginTabletActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** ✅ 학원 QR 생성 (원장용) */
    private void loadStudentsAndMakeQr(String academyNumber, String token) {
        academyApi.getStudentsByAcademy("Bearer " + token, academyNumber)
                .enqueue(new Callback<List<Student>>() {
                    @Override
                    public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(QrLoginTabletActivity.this, "학생 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                            makeQrOnlyAcademy(academyNumber);
                            return;
                        }

                        List<String> ids = new ArrayList<>();
                        for (Student s : response.body()) ids.add(s.getStudentId());

                        String json = "{\"academyNumber\":\"" + academyNumber + "\",\"students\":" + ids.toString() + "}";
                        makeQrFromString(json);
                    }

                    @Override
                    public void onFailure(Call<List<Student>> call, Throwable t) {
                        Toast.makeText(QrLoginTabletActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
                        makeQrOnlyAcademy(academyNumber);
                    }
                });
    }

    /** ✅ QR 이미지 생성 */
    private void makeQrFromString(String qrContent) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 800, 800);
            BarcodeEncoder encoder = new BarcodeEncoder();
            qrImage.setImageBitmap(encoder.createBitmap(matrix));
        } catch (Exception e) {
            Toast.makeText(this, "QR 생성 오류", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeQrOnlyAcademy(String academyNumber) {
        makeQrFromString("academyNumber=" + academyNumber);
    }

    /** ✅ 로그아웃 시 LoginActivity로 이동 */
    private void logout() {
        prefs.edit().clear().apply();
        qrImage.setImageDrawable(null);
        Toast.makeText(this, "로그아웃 완료", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(QrLoginTabletActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /** ✅ 복귀 시 입력창 초기화 */
    @Override
    protected void onResume() {
        super.onResume();
        editId.setText("");
        editPw.setText("");
    }

    private void hideKeyboard() {
        View v = this.getCurrentFocus();
        if (v != null) {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}
