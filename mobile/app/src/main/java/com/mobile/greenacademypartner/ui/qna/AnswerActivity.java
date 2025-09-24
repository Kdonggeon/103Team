package com.mobile.greenacademypartner.ui.qna;

import android.content.SharedPreferences;
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

        // 작성자(옵션) — 필요 없으면 제거 가능
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", ""); // 프로젝트에서 실제로 쓰는 키 확인

        btnPostAnswer.setOnClickListener(v -> {
            String content = etAnswerContent.getText().toString().trim();

            if (content.isEmpty()) {
                Toast.makeText(this, "답변 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            Answer a = new Answer();
            a.setContent(content);
            a.setAuthor(userId); // 서버가 세션에서 author를 정한다면 이 줄은 생략 가능

            String auth = getAuthHeader();
            AnswerApi api = RetrofitClient.getClient().create(AnswerApi.class);
            api.createAnswer(auth, questionId, a).enqueue(new Callback<Answer>() {
                @Override
                public void onResponse(Call<Answer> call, Response<Answer> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AnswerActivity.this, "답변이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AnswerActivity.this, "등록 실패: 서버 오류(" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Answer> call, Throwable t) {
                    Toast.makeText(AnswerActivity.this, "등록 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Authorization 헤더 생성 (다른 화면들과 동일한 방식)
    private String getAuthHeader() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String token = prefs.getString("jwt", null);
        if (token == null || token.isEmpty()) token = prefs.getString("token", null);
        if (token == null || token.isEmpty()) token = prefs.getString("accessToken", null);
        return (token == null || token.isEmpty()) ? null : "Bearer " + token;
    }
}
