package com.mobile.greenacademypartner.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.NoticeApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Notice;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NoticeDetailActivity extends AppCompatActivity {
    private TextView tvTitle, tvAuthor, tvDate, tvContent;
    private NoticeApi api;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_notice_detail);

        tvTitle   = findViewById(R.id.tv_detail_title);
        tvAuthor  = findViewById(R.id.tv_detail_author);
        tvDate    = findViewById(R.id.tv_detail_date);
        tvContent = findViewById(R.id.tv_detail_content);
        api       = RetrofitClient.getClient().create(NoticeApi.class);

        String id = getIntent().getStringExtra("notice_id");
        api.getNotice(id).enqueue(new Callback<Notice>() {
            @Override
            public void onResponse(Call<Notice> c, Response<Notice> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Notice n = r.body();
                    tvTitle.setText(n.getTitle());
                    tvAuthor.setText("작성자: " + n.getAuthor());
                    tvDate.setText("등록일: " + n.getCreatedAt());
                    tvContent.setText(n.getContent());
                } else {
                    Toast.makeText(NoticeDetailActivity.this, "불러오기 실패", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onFailure(Call<Notice> c, Throwable t) {
                Toast.makeText(NoticeDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
