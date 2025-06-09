package com.mobile.greenacademypartner.ui;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.FindIdApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.FindIdRequest;
import com.mobile.greenacademypartner.model.FindIdResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FindIdActivity extends AppCompatActivity {

    private EditText editName, editPhone;
    private RadioGroup radioRole;
    private Button btnFindId;
    private TextView textResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_id);

        editName = findViewById(R.id.edit_name);
        editPhone = findViewById(R.id.edit_phone);
        radioRole = findViewById(R.id.radio_role);
        btnFindId = findViewById(R.id.btn_find_id);
        textResult = findViewById(R.id.text_result);

        btnFindId.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phoneRaw = editPhone.getText().toString().trim();
            String phone = phoneRaw.replaceAll("[^\\d]", "");  // 숫자만 추출
            String role = getSelectedRole();

            if (name.isEmpty() || phone.isEmpty() || role == null) {
                Toast.makeText(this, "모든 항목을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            FindIdRequest request = new FindIdRequest(name, Long.parseLong(phone), role);

            FindIdApi api = RetrofitClient.getClient().create(FindIdApi.class);
            api.findId(request).enqueue(new Callback<FindIdResponse>() {
                @Override
                public void onResponse(Call<FindIdResponse> call, Response<FindIdResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        textResult.setText("당신의 아이디는: " + response.body().getUserId());
                    } else {
                        textResult.setText("일치하는 계정을 찾을 수 없습니다.");
                    }
                }

                @Override
                public void onFailure(Call<FindIdResponse> call, Throwable t) {
                    Toast.makeText(FindIdActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String getSelectedRole() {
        int id = radioRole.getCheckedRadioButtonId();
        if (id == R.id.radio_student) return "student";
        if (id == R.id.radio_parent) return "parent";
        if (id == R.id.radio_teacher) return "teacher";
        return null;
    }
}
