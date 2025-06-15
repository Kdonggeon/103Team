
package com.mobile.greenacademypartner.ui;

import android.content.Intent;
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

public class EditNoticeActivity extends AppCompatActivity {
    private NoticeApi api;
    private String noticeId;
    private EditText editTitle, editContent;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_notice);

        api = RetrofitClient.getClient().create(NoticeApi.class);
        noticeId = getIntent().getStringExtra("NOTICE_ID");

        editTitle   = findViewById(R.id.edit_notice_title);
        editContent = findViewById(R.id.edit_notice_content);
        btnSave     = findViewById(R.id.btn_save);

        // 기존 공지 불러오기
        api.getNotice(noticeId).enqueue(new Callback<Notice>() {
            @Override
            public void onResponse(Call<Notice> call, Response<Notice> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    editTitle.setText(resp.body().getTitle());
                    editContent.setText(resp.body().getContent());
                }
            }
            @Override public void onFailure(Call<Notice> call, Throwable t) { }
        });

        btnSave.setOnClickListener(v -> {
            Notice updated = new Notice();
            updated.setTitle(editTitle.getText().toString());
            updated.setContent(editContent.getText().toString());
            api.updateNotice(noticeId, updated).enqueue(new Callback<Notice>() {
                @Override
                public void onResponse(Call<Notice> call, Response<Notice> resp) {
                    if (resp.isSuccessful()) {
                        Toast.makeText(EditNoticeActivity.this, "수정 완료", Toast.LENGTH_SHORT).show();

                        // 🔹 activity_notice로 바로 이동
                        Intent intent = new Intent(EditNoticeActivity.this, NoticeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(EditNoticeActivity.this, "수정 실패", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<Notice> call, Throwable t) {
                    Toast.makeText(EditNoticeActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
