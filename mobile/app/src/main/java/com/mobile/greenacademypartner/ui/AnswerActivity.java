package com.mobile.greenacademypartner.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.AnswerApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Answer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnswerActivity extends AppCompatActivity {

    private EditText etAnswerContent;
    private Button btnPostAnswer;
    private String questionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answers);

        etAnswerContent = findViewById(R.id.et_answer_content);
        btnPostAnswer = findViewById(R.id.btn_post_answer);

        questionId = getIntent().getStringExtra("questionId");

        btnPostAnswer.setOnClickListener(v -> {
            String content = etAnswerContent.getText().toString().trim();

            if (content.isEmpty()) {
                Toast.makeText(this, "답변 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            Answer a = new Answer();
            a.setContent(content);

            AnswerApi api = RetrofitClient.getInstance().create(AnswerApi.class);
            api.createAnswer(questionId, a).enqueue(new Callback<Answer>() {
                @Override
                public void onResponse(Call<Answer> call, Response<Answer> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AnswerActivity.this, "답변이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AnswerActivity.this, "등록 실패: 서버 오류", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Answer> call, Throwable t) {
                    Toast.makeText(AnswerActivity.this, "등록 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
