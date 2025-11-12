package com.mobile.greenacademypartner.ui.qr;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;

public class WaitingRoomActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);

        // ✅ 인텐트로 전달된 데이터 받기
        String studentId = getIntent().getStringExtra("studentId");
        String academyNumber = getIntent().getStringExtra("academyNumber");

        // ✅ 단순히 Toast로만 표시 (XML id 필요 없음)
        if (studentId != null && !studentId.isEmpty()) {
            Toast.makeText(this,
                    "학생 ID: " + studentId + "\n학원 번호: " + academyNumber + "\n출석 완료! 대기 중입니다.",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "출석 완료! 대기 중입니다.", Toast.LENGTH_LONG).show();
        }
    }
}
