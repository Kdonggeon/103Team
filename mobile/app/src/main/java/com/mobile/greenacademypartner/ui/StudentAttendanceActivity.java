package com.mobile.greenacademypartner.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Attendance;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentAttendanceActivity extends AppCompatActivity {

    private TextView attendanceInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        attendanceInfo = findViewById(R.id.attendance_info);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String studentId = prefs.getString("studentId", "");

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);

        Call<List<Attendance>> call = api.getAttendanceForStudent(studentId);

        call.enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StringBuilder sb = new StringBuilder();
                    for (Attendance a : response.body()) {
                        sb.append("수업: ").append(a.getClassId())
                                .append("\n날짜: ").append(a.getDate())
                                .append("\n출석: ").append("출석".equals(a.getStatus()) ? "출석" : "결석")
                                .append("\n\n");
                    }
                    attendanceInfo.setText(sb.toString());
                }
            }

            @Override
            public void onFailure(Call<List<Attendance>> call, Throwable t) {
                Log.e("StudentAttendance", "출석 정보 조회 실패", t);
            }
        });
    }
}
