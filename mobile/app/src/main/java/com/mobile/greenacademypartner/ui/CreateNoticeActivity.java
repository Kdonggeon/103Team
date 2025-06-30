package com.mobile.greenacademypartner.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.NoticeApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Notice;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateNoticeActivity extends AppCompatActivity {
    private EditText etTitle, etContent;
    private Button btnSubmit;
    private NoticeApi api;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_create_notice);

        etTitle   = findViewById(R.id.edit_notice_title);
        etContent = findViewById(R.id.edit_notice_content);
        btnSubmit = findViewById(R.id.btn_notice_submit);
        api       = RetrofitClient.getClient().create(NoticeApi.class);

        btnSubmit.setOnClickListener(v -> {
            String t = etTitle.getText().toString().trim();
            String c = etContent.getText().toString().trim();
            if (t.isEmpty() || c.isEmpty()) {
                Toast.makeText(this, "제목/내용 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            Notice notice = new Notice();
            notice.setTitle(t);
            notice.setContent(c);
            // SharedPreferences에 저장된 로그인 사용자명 꺼내서 세팅
            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            String username = prefs.getString("username", "");
            notice.setAuthor(username);

            api.createNotice(notice).enqueue(new Callback<Notice>() {
                @Override
                public void onResponse(Call<Notice> call, Response<Notice> r) {
                    if (r.isSuccessful()) {
                        Toast.makeText(CreateNoticeActivity.this, "등록 성공!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(CreateNoticeActivity.this, "실패: " + r.code(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Notice> call, Throwable t) {
                    Toast.makeText(CreateNoticeActivity.this, "오류", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
