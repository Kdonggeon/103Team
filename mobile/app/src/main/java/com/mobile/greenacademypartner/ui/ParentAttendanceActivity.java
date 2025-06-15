package com.mobile.greenacademypartner.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.mobile.greenacademypartner.R;

public class ParentAttendanceActivity extends AppCompatActivity {

    private TextView placeholder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_attendance);

        placeholder = findViewById(R.id.parent_text);
        placeholder.setText("여기에 자녀 출석 조회 기능을 추가하세요.");
    }
}
