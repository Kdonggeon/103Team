package com.mobile.greenacademypartner.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.FindIdApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.login.FindIdRequest;
import com.mobile.greenacademypartner.model.login.FindIdResponse;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FindIdActivity extends AppCompatActivity {

    private EditText editName, editPhone;
    private Spinner spinnerRole;
    private Button btnFindId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_id);

        editName = findViewById(R.id.edit_name);
        editPhone = findViewById(R.id.edit_phone);
        spinnerRole = findViewById(R.id.spinner_role);
        btnFindId = findViewById(R.id.btn_find_id);

        btnFindId.setOnClickListener(v -> {
            // 입력값 수집
            String name = editName.getText().toString();
            String phone = editPhone.getText().toString();
            String role = spinnerRole.getSelectedItem().toString().toLowerCase(); // student, parent, teacher

            // ✅ 입력값 정제
            String cleanedName = name.trim();
            String cleanedPhone = phone.trim(); // ✅ 하이픈(-) 포함 그대로 서버로 전송


            // 필수 입력 검사
            if (cleanedName.isEmpty() || cleanedPhone.isEmpty()) {
                Toast.makeText(this, "이름과 전화번호를 모두 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 요청 객체 생성
            FindIdRequest request = new FindIdRequest(cleanedName, cleanedPhone, role);

            // API 호출
            FindIdApi api = RetrofitClient.getClient().create(FindIdApi.class);

            api.findId(request).enqueue(new Callback<FindIdResponse>() {
                @Override
                public void onResponse(Call<FindIdResponse> call, Response<FindIdResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String foundId = response.body().getUserId();
                        Intent intent = new Intent(FindIdActivity.this, FindIdResultActivity.class);
                        intent.putExtra("userId", response.body().getUserId()); // 응답에서 받은 아이디
                        startActivity(intent);

                    } else {
                        Toast.makeText(FindIdActivity.this, "해당 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<FindIdResponse> call, Throwable t) {
                    Toast.makeText(FindIdActivity.this, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
        ThemeColorUtil.applyThemeColor(this);
    }
}
