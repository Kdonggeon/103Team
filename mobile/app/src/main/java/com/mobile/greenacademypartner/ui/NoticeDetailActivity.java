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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_detail);

        tvTitle   = findViewById(R.id.tv_detail_title);
        tvAuthor  = findViewById(R.id.tv_detail_author);
        tvDate    = findViewById(R.id.tv_detail_date);
        tvContent = findViewById(R.id.tv_detail_content);

        api = RetrofitClient.getClient().create(NoticeApi.class);
        String id = getIntent().getStringExtra("notice_id");

        api.getNotice(id).enqueue(new Callback<Notice>() {
            @Override
            public void onResponse(Call<Notice> call, Response<Notice> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Notice n = response.body();
                    tvTitle.setText(n.getTitle());
                    tvAuthor.setText("작성자: " + n.getAuthor());

                    // 년월일만 추출하여 포맷
                    String formattedDate = formatDate(n.getCreatedAt());
                    tvDate.setText("등록일: " + formattedDate);

                    tvContent.setText(n.getContent());
                } else {
                    Toast.makeText(NoticeDetailActivity.this,
                            "불러오기 실패", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Notice> call, Throwable t) {
                Toast.makeText(NoticeDetailActivity.this,
                        "네트워크 오류", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * 날짜 문자열에서 "yyyy-MM-dd" 부분만 추출하여 "yyyy년 MM월 dd일"로 변환
     */
    private String formatDate(String raw) {
        // raw 예: "2025-06-11T09:23:45Z" 또는 "2025-06-11"
        String datePart = raw != null && raw.length() >= 10 ? raw.substring(0, 10) : raw;
        if (datePart != null) {
            String[] parts = datePart.split("-");
            if (parts.length >= 3) {
                return parts[0] + "/"
                        + parts[1] + "/"
                        + parts[2];
            }
        }
        // 포맷 불가 시 원본 문자열 반환
        return raw;
    }
}
