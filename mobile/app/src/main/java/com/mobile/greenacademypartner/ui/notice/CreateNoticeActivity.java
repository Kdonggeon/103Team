package com.mobile.greenacademypartner.ui.notice;

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
    // 제목, 내용 입력란 및 제출 버튼
    private EditText etTitle, etContent;
    private Button btnSubmit;
    // 공지사항 API 인터페이스
    private NoticeApi api;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_create_notice);


        // UI 요소 초기화
        etTitle   = findViewById(R.id.edit_notice_title);
        etContent = findViewById(R.id.edit_notice_content);
        btnSubmit = findViewById(R.id.btn_notice_submit);

        // Retrofit을 이용해 API 인스턴스 생성
        api       = RetrofitClient.getClient().create(NoticeApi.class);

        //제출 버튼 클릭 시
        btnSubmit.setOnClickListener(v -> {
            String t = etTitle.getText().toString().trim();
            String c = etContent.getText().toString().trim();

            //제목이 없으면 경고
            if (t.isEmpty() || c.isEmpty()) {
                Toast.makeText(this, "제목/내용 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }


            //공지사항 객체를 생성하고 값 설정
            Notice notice = new Notice();
            notice.setTitle(t);
            notice.setContent(c);

            // 로그인 정보에서 작성자명 가져오기(작성자 표시에 필요)

            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            String name = prefs.getString("name", "");
            notice.setAuthor(name);

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
