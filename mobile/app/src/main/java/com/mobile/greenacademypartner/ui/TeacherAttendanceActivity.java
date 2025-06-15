package com.mobile.greenacademypartner.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.mobile.greenacademypartner.R;

public class TeacherAttendanceActivity extends AppCompatActivity {

    private TextView placeholder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_attendance);

        placeholder = findViewById(R.id.teacher_text);
        placeholder.setText("여기에 교사용 출석관리 기능을 추가하세요.");
    }
}
