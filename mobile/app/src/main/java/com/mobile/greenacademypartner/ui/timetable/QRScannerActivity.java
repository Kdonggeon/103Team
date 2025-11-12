package com.mobile.greenacademypartner.ui.timetable;

<<<<<<< HEAD
import android.content.Intent;
import android.content.SharedPreferences;
=======
>>>>>>> new2
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

import java.util.Set;

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

<<<<<<< HEAD
    private static final String TAG = "QRScanner";
    private RoomApi api;
=======
    private RoomApi roomApi;
>>>>>>> new2

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

<<<<<<< HEAD
        api = RetrofitClient.getClient().create(RoomApi.class);

        // ZXing 스캐너 실행
=======
        // ✅ Retrofit 초기화
        roomApi = RetrofitClient.getClient().create(RoomApi.class);

        // ✅ QR 스캔 시작
>>>>>>> new2
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("QR 코드를 스캔하세요");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.initiateScan();
<<<<<<< HEAD

        // 디버그: 인텐트 extras 키 확인 (원인 추적용)
        if (getIntent() != null && getIntent().getExtras() != null) {
            Set<String> keys = getIntent().getExtras().keySet();
            Log.d(TAG, "Intent extras keys: " + keys);
        } else {
            Log.d(TAG, "Intent has no extras");
        }
=======
>>>>>>> new2
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
<<<<<<< HEAD
            // 전체 URL이든 "room=1&..." 조각이든 모두 처리
            Uri uri = Uri.parse(qrData);
            if (uri.getQueryParameter("room") == null && qrData.contains("=")) {
                uri = Uri.parse("?" + qrData);
            }

            String roomStr = uri.getQueryParameter("room");
            String academyStr = uri.getQueryParameter("academyNumber");
            String seatStr = uri.getQueryParameter("seat");
            String idxStr = uri.getQueryParameter("idx"); // 0-based일 수 있음

            if (roomStr == null || academyStr == null) {
                Toast.makeText(this, "QR에 필수 정보(room/academyNumber)가 없습니다.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Missing params: room=" + roomStr + ", academyNumber=" + academyStr + " / raw=" + qrData);
                finish();
                return;
            }

            int roomNumber = Integer.parseInt(roomStr);
            int academyNumber = Integer.parseInt(academyStr);

            int seatNumber;
            if (seatStr != null) {
                seatNumber = Integer.parseInt(seatStr);
            } else if (idxStr != null) {
                seatNumber = Integer.parseInt(idxStr) + 1; // 0-based → 1-based
            } else {
                Toast.makeText(this, "좌석 정보(seat/idx)가 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if (seatNumber <= 0) {
                Toast.makeText(this, "좌석 번호가 유효하지 않습니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // 1) 인텐트에서 studentId 우선 획득
            String studentId = getIntent().getStringExtra("studentId");

            // 2) 못 받았으면 SharedPreferences에서 최종 복원 (문자/정수 모두 커버)
            if (studentId == null || studentId.trim().isEmpty()) {
                studentId = restoreStudentIdFromPrefs();
            }

            if (studentId == null || studentId.trim().isEmpty()) {
                Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "studentId missing (intent & prefs both empty)");
                finish();
                return;
            }

            Log.d(TAG, "studentId=" + studentId + ", academy=" + academyNumber + ", room=" + roomNumber + ", seat=" + seatNumber);

            // 출석 API 호출 (academyNumber 포함)
            Call<ResponseBody> call = api.checkIn(roomNumber, academyNumber, seatNumber, studentId);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(QRScannerActivity.this, "출석이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        String msg = "출석 실패 (" + response.code() + ")";
                        try {
                            if (response.errorBody() != null) {
                                msg += " - " + response.errorBody().string();
                            }
                        } catch (Exception ignored) {}
                        Toast.makeText(QRScannerActivity.this, msg, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Check-in failed: " + msg);
                    }
                    finish();
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(QRScannerActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "출석 API 실패", t);
                    finish();
                }
            });
=======
            // ✅ JSON 형태라면 학원 출석 QR
            if (qrData.trim().startsWith("{")) {
                handleAcademyQR(qrData);
                return;
            }

            // ✅ 아니면 기존 좌석 QR
            handleSeatQR(qrData);
>>>>>>> new2

        } catch (Exception e) {
            Toast.makeText(this, "QR 코드 형식이 잘못되었습니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "QR 파싱 오류", e);
            finish();
        }
    }

<<<<<<< HEAD
    /** 여러 프리퍼런스/키 조합에서 studentId 복원 (문자/정수 모두 지원) */
    private String restoreStudentIdFromPrefs() {
        String[] prefNames = {"login_prefs", "session", "login"};
        String[] strKeys   = {"username", "studentId", "Student_ID", "StudentId"};
        for (String p : prefNames) {
            try {
                SharedPreferences sp = getSharedPreferences(p, MODE_PRIVATE);
                // 문자열 우선
                for (String k : strKeys) {
                    String v = sp.getString(k, null);
                    if (v != null && !v.trim().isEmpty()) return v.trim();
                }
                // 정수로 저장된 케이스까지 회수
                if (sp.contains("studentId")) {
                    int iv = sp.getInt("studentId", Integer.MIN_VALUE);
                    if (iv != Integer.MIN_VALUE) return String.valueOf(iv);
                }
                if (sp.contains("Student_ID")) {
                    int iv = sp.getInt("Student_ID", Integer.MIN_VALUE);
                    if (iv != Integer.MIN_VALUE) return String.valueOf(iv);
                }
                if (sp.contains("StudentId")) {
                    int iv = sp.getInt("StudentId", Integer.MIN_VALUE);
                    if (iv != Integer.MIN_VALUE) return String.valueOf(iv);
                }
            } catch (Exception ignore) {}
        }
        return null;
=======
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
>>>>>>> new2
    }
}
