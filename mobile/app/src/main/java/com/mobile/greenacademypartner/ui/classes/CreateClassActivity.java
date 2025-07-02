package com.mobile.greenacademypartner.ui.classes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.model.classes.CreateClassRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateClassActivity extends AppCompatActivity {

    private EditText editClassName, editSchedule;
    private Button buttonCreate;
    private TeacherApi api;
    private String teacherId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_class);

        editClassName = findViewById(R.id.edit_class_name);
        editSchedule = findViewById(R.id.edit_schedule);
        buttonCreate = findViewById(R.id.button_create_class);

        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        teacherId = prefs.getString("username", null); // 교사 ID

        api = RetrofitClient.getClient().create(TeacherApi.class);

        buttonCreate.setOnClickListener(v -> {
            String name = editClassName.getText().toString().trim();
            String schedule = editSchedule.getText().toString().trim();

            if (name.isEmpty() || schedule.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            CreateClassRequest request = new CreateClassRequest(name, teacherId, schedule);
            api.createClass(request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(CreateClassActivity.this, "수업이 생성되었습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(CreateClassActivity.this, "수업 생성 실패", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(CreateClassActivity.this, "서버 오류 발생", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
