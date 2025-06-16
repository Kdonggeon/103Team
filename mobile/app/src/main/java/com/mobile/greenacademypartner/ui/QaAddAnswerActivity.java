package com.mobile.greenacademypartner.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.QaAnswerApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.QaAnswer;

import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QaAddAnswerActivity extends AppCompatActivity {
    private EditText etAnswerContent;
    private Button btnSaveAnswer;
    private QaAnswerApi answerApi;
    private String qaId, userId, userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qa_answer);

        Toolbar toolbar = findViewById(R.id.toolbar_qa_answer);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etAnswerContent = findViewById(R.id.etAnswerContent);
        btnSaveAnswer = findViewById(R.id.btnSaveAnswer);

        answerApi = RetrofitClient.getClient().create(QaAnswerApi.class);

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        userRole = prefs.getString("role", "");

        qaId = getIntent().getStringExtra("qa_id");

        btnSaveAnswer.setOnClickListener(v -> {
            String content = etAnswerContent.getText().toString().trim();
            if (content.isEmpty()) {
                etAnswerContent.setError("답변 내용을 입력해주세요");
                return;
            }

            QaAnswer answer = new QaAnswer();
            answer.setContent(content);
            answer.setAuthorId(userId);
            answer.setAuthorRole(userRole);
            answer.setCreatedAt(new Date());
            answer.setUpdatedAt(new Date());
            answer.setQaId(qaId);  // 반드시 포함되어야 함

            // ✅ 변경된 실제 API 메서드 호출
            answerApi.createAnswer(answer).enqueue(new Callback<QaAnswer>() {
                @Override
                public void onResponse(Call<QaAnswer> call, Response<QaAnswer> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(QaAddAnswerActivity.this, "답변이 등록되었습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(QaAddAnswerActivity.this, "답변 등록 실패 (서버 오류)", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<QaAnswer> call, Throwable t) {
                    Toast.makeText(QaAddAnswerActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                }
            });
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
