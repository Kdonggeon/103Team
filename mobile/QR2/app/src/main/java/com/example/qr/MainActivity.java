package com.example.qr;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qr.ui.login.QrLoginTabletActivity; // ← 로그인 Activity 경로에 맞게 수정

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 바로 로그인 화면으로 이동
        Intent intent = new Intent(MainActivity.this, QrLoginTabletActivity.class);
        startActivity(intent);
        finish(); // MainActivity 닫기
    }
}
