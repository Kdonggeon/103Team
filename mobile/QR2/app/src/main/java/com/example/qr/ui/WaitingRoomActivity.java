package com.example.qr.ui;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qr.R;

public class WaitingRoomActivity extends AppCompatActivity {

    private static final int AUTO_CLOSE_MS = 1000; // 1초 뒤 자동 종료

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);

        String studentId = getIntent().getStringExtra("studentId");
        TextView tv = findViewById(R.id.txt_waiting_message);

        if (studentId != null && !studentId.isEmpty()) {
            tv.setText("학생 ID: " + studentId + " 님이 대기실로 이동했습니다.");
        } else {
            tv.setText("대기실로 이동했습니다.");
        }

        // ✅ 화면 아무 곳이나 터치하면 닫기
        findViewById(android.R.id.content).setOnClickListener(v -> finish());

        // 1초 뒤 자동으로 이전 화면으로 돌아가도록
        new Handler().postDelayed(this::finish, AUTO_CLOSE_MS);
    }
}
