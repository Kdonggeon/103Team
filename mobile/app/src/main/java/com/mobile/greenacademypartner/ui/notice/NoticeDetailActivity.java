package com.mobile.greenacademypartner.ui.notice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.NoticeApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Notice;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class    NoticeDetailActivity extends AppCompatActivity {
    private NoticeApi api;
    private String noticeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_detail);
        Toolbar toolbar = findViewById(R.id.toolbar_notice_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        noticeId = getIntent().getStringExtra("NOTICE_ID");
        Log.d("NoticeDetail", "noticeId = " + noticeId);
        if (noticeId == null || noticeId.trim().isEmpty()) {
            Toast.makeText(this, "공지 ID가 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        api = RetrofitClient.getClient().create(NoticeApi.class);


        TextView tvTitle   = findViewById(R.id.tv_detail_title);
        TextView tvContent = findViewById(R.id.tv_detail_content);
        TextView tvAuthor  = findViewById(R.id.tv_detail_author);
        TextView tvDate    = findViewById(R.id.tv_detail_date);
        Button   btnEdit   = findViewById(R.id.btn_edit);
        Button   btnDelete = findViewById(R.id.btn_delete);

        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", "");
        if (!"teacher".equals(role)) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }


        api.getNotice(noticeId).enqueue(new Callback<Notice>() {
            @Override
            public void onResponse(Call<Notice> call, Response<Notice> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    Notice n = resp.body();
                    tvTitle.setText(n.getTitle());
                    tvContent.setText(n.getContent());

                    // 작성자 이름 표시: author가 비어있으면 teacherName 사용
                    String displayName = (n.getAuthor() != null && !n.getAuthor().isEmpty())
                            ? n.getAuthor()
                            : n.getTeacherName();
                    tvAuthor.setText(displayName);

                    // 날짜 포맷팅
                    String formatted = n.getCreatedAt();
                    if (formatted != null) {
                        try {
                            Date parsed = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                    .parse(formatted);
                            if (parsed != null) {
                                formatted = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(parsed);
                            }
                        } catch (ParseException e) {
                            Log.w("NoticeDetail", "날짜 파싱 실패: " + formatted);
                        }
                    }
                    tvDate.setText(formatted);
                } else {
                    Log.e("NoticeDetail", "공지 조회 실패, code=" + resp.code());
                    Toast.makeText(NoticeDetailActivity.this, "공지 조회 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Notice> call, Throwable t) {
                Log.e("NoticeDetail", "네트워크 오류", t);
                Toast.makeText(NoticeDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditNoticeActivity.class);
            intent.putExtra("NOTICE_ID", noticeId);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("공지사항 삭제")
                .setMessage("정말 삭제하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> {
                    api.deleteNotice(noticeId).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> r) {
                            if (r.isSuccessful()) {
                                Toast.makeText(NoticeDetailActivity.this, "삭제되었습니다", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(NoticeDetailActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.e("NoticeDetail", "삭제 네트워크 오류", t);
                            Toast.makeText(NoticeDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("취소", null)
                .show()
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
