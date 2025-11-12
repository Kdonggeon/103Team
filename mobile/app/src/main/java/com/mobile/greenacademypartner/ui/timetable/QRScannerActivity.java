package com.mobile.greenacademypartner.ui.timetable;

import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRScannerActivity extends AppCompatActivity {

    private static final String TAG = "QRScanner";
    private RoomApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        api = RetrofitClient.getClient().create(RoomApi.class);

        // ZXing 스캐너 실행
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("좌석 QR 코드를 스캔하세요");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.initiateScan();

        // 디버그: 인텐트 extras 키 확인 (원인 추적용)
        if (getIntent() != null && getIntent().getExtras() != null) {
            Set<String> keys = getIntent().getExtras().keySet();
            Log.d(TAG, "Intent extras keys: " + keys);
        } else {
            Log.d(TAG, "Intent has no extras");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {
            handleQRResult(result.getContents());
        } else {
            Toast.makeText(this, "스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleQRResult(String qrData) {
        try {
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

        } catch (Exception e) {
            Toast.makeText(this, "QR 코드 형식이 잘못되었습니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "QR 파싱 오류", e);
            finish();
        }
    }

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
    }
}
