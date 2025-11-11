package com.example.qr.ui.qr;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import com.example.qr.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class AcademyQrActivity extends AppCompatActivity {

    private ImageView qrImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_academy_qr);

        // ✅ XML의 ImageView id와 정확히 맞춤
        qrImage = findViewById(R.id.img_academy_qr);

        // ✅ QR에 넣을 학원 코드
        String academyNumber = "1203";

        String qrContent = "academyNumber=" + academyNumber;

        // ✅ QR 생성
        Bitmap qrBitmap = generateQRCode(qrContent);
        if (qrBitmap != null) {
            qrImage.setImageBitmap(qrBitmap);
        }
    }

    // ✅ QR 코드 생성 함수
    private Bitmap generateQRCode(String text) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 600, 600);

            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.createBitmap(matrix); // ✅ QR Bitmap 생성

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
