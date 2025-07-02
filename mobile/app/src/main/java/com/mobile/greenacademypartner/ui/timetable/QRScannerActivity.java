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

    private void handleQRResult(String qrData) {
        try {
            // 예: "room=12&seat=2&student=s1002"
            Uri uri = Uri.parse("?" + qrData);
            int roomNumber = Integer.parseInt(uri.getQueryParameter("room"));
            int seatNumber = Integer.parseInt(uri.getQueryParameter("seat"));
            String studentId = uri.getQueryParameter("student");

            // 출석 API 호출
            Call<ResponseBody> call = api.checkIn(roomNumber, seatNumber, studentId);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(QRScannerActivity.this, "출석이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(QRScannerActivity.this, "출석에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(QRScannerActivity.this, "네트워크 오류 발생", Toast.LENGTH_SHORT).show();
                    Log.e("QR", "출석 API 실패", t);
                    finish();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "QR 코드 형식이 잘못되었습니다.", Toast.LENGTH_SHORT).show();
            Log.e("QR", "QR 파싱 오류", e);
            finish();
        }
    }
}
