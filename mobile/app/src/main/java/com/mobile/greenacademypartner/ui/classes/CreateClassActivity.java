package com.mobile.greenacademypartner.ui.classes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
// import com.mobile.greenacademypartner.api.RetrofitClient;         // 🔕 [DISABLED] 네트워크 호출 제거
// import com.mobile.greenacademypartner.api.TeacherApi;            // 🔕 [DISABLED] 교사 API 제거
import com.mobile.greenacademypartner.model.classes.CreateClassRequest; // 사용 안 해도 남겨둠 (참조 코드 주석용)
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

// import retrofit2.Call;
// import retrofit2.Callback;
// import retrofit2.Response;

public class CreateClassActivity extends AppCompatActivity {

    private EditText editClassName, editSchedule;
    private Button buttonCreate;
    // private TeacherApi api;                                      // 🔕 [DISABLED]
    private String teacherId;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_class);

        editClassName = findViewById(R.id.edit_class_name);
        editSchedule  = findViewById(R.id.edit_schedule);
        buttonCreate  = findViewById(R.id.button_create_class);

        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        role      = prefs.getString("role", "unknown");
        teacherId = prefs.getString("username", null); // (기존 교사 ID)

        // 🔒 teacher/director 기능 제거: 화면 자체를 막고 종료
        if (!"student".equalsIgnoreCase(role) && !"parent".equalsIgnoreCase(role)) {
            // (teacher/director 포함) 허용하지 않는 역할
            Toast.makeText(this, "이 기능은 더 이상 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 🔕 [DISABLED] 교사 기능용 API 인스턴스
        // api = RetrofitClient.getClient().create(TeacherApi.class);

        // 버튼 클릭 시에도 동작 막기
        buttonCreate.setOnClickListener(v -> {
            // 기존 입력 검증 로직은 남겨두되, 실제 네트워크 호출은 막기
            String name     = editClassName.getText().toString().trim();
            String schedule = editSchedule.getText().toString().trim();

            if (name.isEmpty() || schedule.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔕 [DISABLED] 실제 생성 요청
            // CreateClassRequest request = new CreateClassRequest(name, teacherId, schedule);
            // api.createClass(request).enqueue(new Callback<Void>() {
            //     @Override
            //     public void onResponse(Call<Void> call, Response<Void> response) {
            //         if (response.isSuccessful()) {
            //             Toast.makeText(CreateClassActivity.this, "수업이 생성되었습니다", Toast.LENGTH_SHORT).show();
            //             finish();
            //         } else {
            //             Toast.makeText(CreateClassActivity.this, "수업 생성 실패", Toast.LENGTH_SHORT).show();
            //         }
            //     }
            //
            //     @Override
            //     public void onFailure(Call<Void> call, Throwable t) {
            //         Toast.makeText(CreateClassActivity.this, "서버 오류 발생", Toast.LENGTH_SHORT).show();
            //     }
            // });

            // 👉 안내만 표시 (교사 기능 제거)
            Toast.makeText(this, "수업 생성은 현재 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
        });

        ThemeColorUtil.applyThemeColor(this);
    }
}
