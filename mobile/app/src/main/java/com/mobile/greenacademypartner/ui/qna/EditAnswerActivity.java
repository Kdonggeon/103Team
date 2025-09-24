package com.mobile.greenacademypartner.ui.qna;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

public class EditAnswerActivity extends AppCompatActivity {

    private EditText editTextAnswer;
    private Button btnSave;

    private String answerId;
    private String questionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_answer);

        editTextAnswer = findViewById(R.id.et_edit_answer_content);
        btnSave = findViewById(R.id.btn_save_answer);

        answerId = getIntent().getStringExtra("answerId");
        questionId = getIntent().getStringExtra("questionId");

        Log.d("EditAnswerActivity", "[onCreate] answerId=" + answerId + ", questionId=" + questionId);

        if (answerId != null && !answerId.isEmpty()) {
            loadAnswerDetail(answerId);
        }

        btnSave.setOnClickListener(v -> {
            String content = editTextAnswer.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "내용을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (answerId != null && !answerId.isEmpty()) {
                updateAnswer(answerId, content);
            } else {
                if (questionId == null || questionId.isEmpty()) {
                    Toast.makeText(this, "질문 ID가 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                createAnswer(questionId, content);
            }
        });
    }

    private void loadAnswerDetail(String id) {
        Log.d("EditAnswerActivity", "[loadAnswerDetail] id=" + id);
        String auth = getAuthHeader();

        AnswerApi api = RetrofitClient.getClient().create(AnswerApi.class);
        api.getAnswer(auth, id).enqueue(new Callback<Answer>() {
            @Override
            public void onResponse(Call<Answer> call, Response<Answer> response) {
                Log.d("EditAnswerActivity", "[loadAnswerDetail] code=" + response.code());
                if (response.code() == 403) {
                    Toast.makeText(EditAnswerActivity.this, "접근 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    editTextAnswer.setText(response.body().getContent());
                } else {
                    Toast.makeText(EditAnswerActivity.this, "답변을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Answer> call, Throwable t) {
                Log.e("EditAnswerActivity", "[loadAnswerDetail] Network Error", t);
                Toast.makeText(EditAnswerActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void createAnswer(String qid, String content) {
        Log.d("EditAnswerActivity", "[createAnswer] questionId=" + qid);
        String auth = getAuthHeader();

        Answer newAnswer = new Answer();
        newAnswer.setContent(content);

        AnswerApi api = RetrofitClient.getClient().create(AnswerApi.class);
        api.createAnswer(auth, qid, newAnswer).enqueue(new Callback<Answer>() {
            @Override
            public void onResponse(Call<Answer> call, Response<Answer> response) {
                Log.d("EditAnswerActivity", "[createAnswer] code=" + response.code());
                if (response.code() == 403) {
                    Toast.makeText(EditAnswerActivity.this, "접근 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                if (response.isSuccessful()) {
                    Toast.makeText(EditAnswerActivity.this, "등록 완료", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditAnswerActivity.this, "등록 실패(" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Answer> call, Throwable t) {
                Log.e("EditAnswerActivity", "[createAnswer] Network Error", t);
                Toast.makeText(EditAnswerActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAnswer(String id, String content) {
        Log.d("EditAnswerActivity", "[updateAnswer] id=" + id);
        String auth = getAuthHeader();

        Answer updatedAnswer = new Answer();
        updatedAnswer.setContent(content);

        AnswerApi api = RetrofitClient.getClient().create(AnswerApi.class);
        api.updateAnswer(auth, id, updatedAnswer).enqueue(new Callback<Answer>() {
            @Override
            public void onResponse(Call<Answer> call, Response<Answer> response) {
                Log.d("EditAnswerActivity", "[updateAnswer] code=" + response.code());
                if (response.code() == 403) {
                    Toast.makeText(EditAnswerActivity.this, "접근 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                if (response.isSuccessful()) {
                    Toast.makeText(EditAnswerActivity.this, "수정 완료", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditAnswerActivity.this, "수정 실패(" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Answer> call, Throwable t) {
                Log.e("EditAnswerActivity", "[updateAnswer] Network Error", t);
                Toast.makeText(EditAnswerActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getAuthHeader() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String token = prefs.getString("jwt", null);
        if (token == null || token.isEmpty()) token = prefs.getString("token", null);
        if (token == null || token.isEmpty()) token = prefs.getString("accessToken", null);
        return (token == null || token.isEmpty()) ? null : "Bearer " + token;
    }
}
