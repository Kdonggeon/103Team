package com.mobile.greenacademypartner.ui.timetable;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.RoomApi;
import com.mobile.greenacademypartner.api.AttendanceApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ✅ QR 스캐너 통합 버전 (대기실 이동 없음)
 * - 학원 출석 QR(JSON): {"academyNumber":"103","students":["12345","1111"]}
 * - 좌석 출석 QR(기존 key=value): room=12&seat=2&student=s1002
 */
public class QRScannerActivity extends AppCompatActivity {

    private RoomApi roomApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Retrofit 초기화
        roomApi = RetrofitClient.getClient().create(RoomApi.class);

        // ✅ QR 스캔 시작
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("QR 코드를 스캔하세요");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {
            handleQRResult(result.getContents());
        } else {
            Toast.makeText(this, "스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /** ✅ 스캔된 QR 문자열 분석 (자동 분기) */
    private void handleQRResult(String qrData) {
        try {
            // ✅ JSON 형태라면 학원 출석 QR
            if (qrData.trim().startsWith("{")) {
                handleAcademyQR(qrData);
                return;
            }

            // ✅ 아니면 기존 좌석 QR
            handleSeatQR(qrData);

        } catch (Exception e) {
            Toast.makeText(this, "QR 코드 형식이 잘못되었습니다.", Toast.LENGTH_SHORT).show();
            Log.e("QR", "QR 파싱 오류", e);
            finish();
        }
    }

    /** ✅ 기존 좌석 출석용 QR 처리 */
    private void handleSeatQR(String qrData) {
        try {
            Uri uri = Uri.parse("?" + qrData);
            int roomNumber = Integer.parseInt(uri.getQueryParameter("room"));
            int seatNumber = Integer.parseInt(uri.getQueryParameter("seat"));
            String studentId = uri.getQueryParameter("student");

            Call<ResponseBody> call = roomApi.checkIn(roomNumber, seatNumber, studentId);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(QRScannerActivity.this, "💺 좌석 출석 완료!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(QRScannerActivity.this, "좌석 출석 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(QRScannerActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
                    Log.e("QR", "좌석 출석 실패", t);
                    finish();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "좌석 QR 형식 오류", Toast.LENGTH_SHORT).show();
            Log.e("QR", "좌석 QR 파싱 오류", e);
            finish();
        }
    }

    /** ✅ 새 학원 출석용 QR 처리 (대기실 이동 없음) */
    private void handleAcademyQR(String qrData) {
        try {
            JSONObject qrJson = new JSONObject(qrData);
            String academyNumber = qrJson.getString("academyNumber");
            JSONArray students = qrJson.getJSONArray("students");

            // ✅ 현재 로그인한 학생 ID & 토큰 불러오기
            String studentId = getSharedPreferences("login_prefs", MODE_PRIVATE)
                    .getString("student_id", "");
            String token = getSharedPreferences("login_prefs", MODE_PRIVATE)
                    .getString("token", "");

            if (studentId.isEmpty() || token.isEmpty()) {
                Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // ✅ QR 목록에 내 ID가 포함되어 있는지 확인
            boolean valid = false;
            for (int i = 0; i < students.length(); i++) {
                if (studentId.equals(students.getString(i))) {
                    valid = true;
                    break;
                }
            }

            if (!valid) {
                Toast.makeText(this, "이 학원 학생이 아닙니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // ✅ 출석 체크 요청
            AttendanceApi attendanceApi = RetrofitClient.getClient().create(AttendanceApi.class);
            Map<String, String> req = new HashMap<>();
            req.put("academyNumber", academyNumber);
            req.put("studentId", studentId);

            attendanceApi.checkIn("Bearer " + token, req)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(QRScannerActivity.this, "🏫 학원 출석 완료!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(QRScannerActivity.this, "출석 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                            finish(); // ✅ 대기실 이동 없이 바로 종료
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(QRScannerActivity.this, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });

        } catch (Exception e) {
            Toast.makeText(this, "학원 QR 형식 오류", Toast.LENGTH_SHORT).show();
            Log.e("QR", "학원 QR 파싱 오류", e);
            finish();
        }
    }
}
