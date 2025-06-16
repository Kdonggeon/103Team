package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.QaAnswerApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.QaAnswer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QaDetailActivity extends AppCompatActivity {
    private TextView tvTitle, tvContent, tvAnswer;
    private Button btnAddAnswer;
    private QaAnswerApi answerApi;
    private String qaId, userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qa_detail);

        // 1. 툴바
        Toolbar toolbar = findViewById(R.id.toolbar_qa_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // 2. 뷰 연결
        tvTitle = findViewById(R.id.tv_detail_title);
        tvContent = findViewById(R.id.tv_detail_content);
        tvAnswer = findViewById(R.id.tv_detail_answer_content);
        btnAddAnswer = findViewById(R.id.btn_add_answer);

        // 3. 사용자 정보
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        userRole = prefs.getString("role", "");

        // 4. 학생/학부모인 경우 버튼 숨김
        if ("student".equalsIgnoreCase(userRole) || "parent".equalsIgnoreCase(userRole)) {
            btnAddAnswer.setVisibility(View.GONE);
        }

        // 5. 질문 정보 받기
        qaId = getIntent().getStringExtra("qa_id");
        String title = getIntent().getStringExtra("qa_title");
        String content = getIntent().getStringExtra("qa_content");

        tvTitle.setText(title);
        tvContent.setText(content);

        // 6. 답변 등록 화면으로 이동
        btnAddAnswer.setOnClickListener(v -> {
            Intent intent = new Intent(QaDetailActivity.this, QaAddAnswerActivity.class);
            intent.putExtra("qa_id", qaId);
            startActivity(intent);
        });

        // 7. 기존 답변 불러오기
        answerApi = RetrofitClient.getClient().create(QaAnswerApi.class);
        loadAnswer();
    }

    private void loadAnswer() {
        answerApi.getAnswerByQaId(qaId).enqueue(new Callback<QaAnswer>() {
            @Override
            public void onResponse(Call<QaAnswer> call, Response<QaAnswer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvAnswer.setText(response.body().getContent());
                } else {
                    tvAnswer.setText("답변이 아직 없습니다.");
                }
            }

            @Override
            public void onFailure(Call<QaAnswer> call, Throwable t) {
                tvAnswer.setText("답변 불러오기 실패");
            }
        });
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
