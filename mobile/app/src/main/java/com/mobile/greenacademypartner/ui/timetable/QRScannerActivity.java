// mobile/app/src/main/java/com/mobile/greenacademypartner/ui/timetable/QRScannerActivity.java
package com.mobile.greenacademypartner.ui.timetable;

import android.content.Intent;
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
// 세션에서 학생 아이디를 얻는 유틸 (프로젝트 경로에 맞게 import 경로 확인)
import com.mobile.greenacademypartner.util.SessionUtil;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRScannerActivity extends AppCompatActivity {

    private RoomApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // RoomApi 사용
        api = RetrofitClient.getClient().create(RoomApi.class);

        // QR 스캔 시작
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("좌석 QR 코드를 스캔하세요");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
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

    /** v=1&type=seat&academyNumber=103&room=403&seat=12 형태만 허용 */
    private void handleQRResult(String qrData) {
        try {
            // 쿼리스트링 파싱을 위해 접두사 '?'를 붙여 Uri로 처리
            Uri uri = Uri.parse("?" + qrData);

            String v = uri.getQueryParameter("v");
            String type = uri.getQueryParameter("type");
            String aStr = uri.getQueryParameter("academyNumber");
            String rStr = uri.getQueryParameter("room");
            String sStr = uri.getQueryParameter("seat");

            if (!"1".equals(v) || !"seat".equals(type) || aStr == null || rStr == null || sStr == null) {
                Toast.makeText(this, "QR 코드 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            int academyNumber = Integer.parseInt(aStr);
            int roomNumber = Integer.parseInt(rStr);
            int seatNumber = Integer.parseInt(sStr);

            // 세션에서 학생 아이디 조회 (null이면 로그인 요구)
            String studentId = SessionUtil.getStudentId(this);
            if (studentId == null || studentId.isEmpty()) {
                Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                // 필요 시 로그인 화면으로 이동
                // startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            // 출석/좌석배정 API 호출
            Call<ResponseBody> call = api.checkIn(roomNumber, academyNumber, seatNumber, studentId);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(QRScannerActivity.this, "좌석 배정 완료", Toast.LENGTH_SHORT).show();
                    } else if (response.code() == 409) {
                        Toast.makeText(QRScannerActivity.this, "이미 배정된 좌석입니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(QRScannerActivity.this, "배정 실패(" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(QRScannerActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                    Log.e("QR", "출석 API 실패", t);
                    finish();
                }
            });

        } catch (NumberFormatException nfe) {
            Toast.makeText(this, "QR 숫자 파싱 오류", Toast.LENGTH_SHORT).show();
            Log.e("QR", "숫자 파싱 오류", nfe);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "QR 코드 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            Log.e("QR", "QR 파싱 오류", e);
            finish();
        }
    }
}
