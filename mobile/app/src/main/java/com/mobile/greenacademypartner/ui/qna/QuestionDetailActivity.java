package com.mobile.greenacademypartner.ui.qna;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.AnswerApi;
import com.mobile.greenacademypartner.api.QuestionApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.Answer;
import com.mobile.greenacademypartner.model.Question;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuestionDetailActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private LinearLayout navContainer;
    private TextView mainContentText;

    private Button btnDeleteQuestion;
    private Button btnAddAnswer;

    private TextView tvTitle;
    private TextView tvContent;
    private TextView tvAuthor;
    private TextView tvDate;

    private RecyclerView rvAnswers;
    private AnswerAdapter answerAdapter;

    private String questionId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        questionId = getIntent().getStringExtra("questionId");
        if (questionId == null || questionId.isEmpty()) {
            finish();  // ID 없으면 화면 종료
            return;
        }

        setContentView(R.layout.activity_question_detail);

        // 2) 툴바 및 네비게이션
        toolbar = findViewById(R.id.toolbar_question_detail);
        ThemeColorUtil.applyThemeColor(this, toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        navContainer = findViewById(R.id.nav_container_question_detail);
        mainContentText = findViewById(R.id.main_content_text_question_detail);

        // 3) 버튼
        btnDeleteQuestion = findViewById(R.id.btn_delete_question);
        btnDeleteQuestion.setOnClickListener(v -> deleteQuestion());

        btnAddAnswer = findViewById(R.id.btn_add_answer);
        btnAddAnswer.setOnClickListener(v -> {
            Intent intent = new Intent(QuestionDetailActivity.this, AnswerActivity.class);
            intent.putExtra("questionId", questionId);
            startActivity(intent);
        });

        // 4) 뷰 바인딩
        tvTitle   = findViewById(R.id.tv_question_title);
        tvContent = findViewById(R.id.tv_question_content);
        tvAuthor  = findViewById(R.id.tv_question_author);
        tvDate    = findViewById(R.id.tv_question_date);

        rvAnswers = findViewById(R.id.rv_answers);
        rvAnswers.setLayoutManager(new LinearLayoutManager(this));
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        userRole = prefs.getString("role", "");
        answerAdapter = new AnswerAdapter(this, questionId, userRole);
        rvAnswers.setAdapter(answerAdapter);

        // 5) 데이터 로드
        loadQuestionDetail(questionId);
        loadAnswerList(questionId);

        // 6) 권한에 따라 버튼 숨기기
        if (!"TEACHER".equalsIgnoreCase(userRole)) {
            btnAddAnswer.setVisibility(View.GONE);
            btnDeleteQuestion.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadQuestionDetail(String id) {
        QuestionApi api = RetrofitClient.getClient().create(QuestionApi.class);
        api.getQuestion(id).enqueue(new Callback<Question>() {
            @Override
            public void onResponse(Call<Question> call, Response<Question> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Question q = response.body();

                    tvTitle.setText(q.getTitle());
                    // ← 여기에서 "작성자 ID"를 그대로 노출
                    tvAuthor.setText(q.getAuthor());
                    tvContent.setText(q.getContent());

                    if (q.getCreatedAt() != null && q.getCreatedAt().contains("T")) {
                        String date = q.getCreatedAt().split("T")[0];
                        tvDate.setText(date);
                    } else {
                        tvDate.setText(q.getCreatedAt());
                    }

                    mainContentText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<Question> call, Throwable t) {
                mainContentText.setText("질문을 불러오지 못했습니다.");
            }
        });
    }

    public void loadAnswerList(String questionId) {
        loadAnswerList(questionId, () -> {});
    }

    public void loadAnswerList(String questionId, Runnable onComplete) {
        AnswerApi api = RetrofitClient.getClient().create(AnswerApi.class);
        api.listAnswers(questionId).enqueue(new Callback<List<Answer>>() {
            @Override
            public void onResponse(Call<List<Answer>> call, Response<List<Answer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    answerAdapter.submitList(response.body());
                } else {
                    Toast.makeText(
                            QuestionDetailActivity.this,
                            "답변을 불러오지 못했습니다.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
                onComplete.run();
            }

            @Override
            public void onFailure(Call<List<Answer>> call, Throwable t) {
                Toast.makeText(
                        QuestionDetailActivity.this,
                        "오류: " + t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
                onComplete.run();
            }
        });
    }

    private void deleteQuestion() {
        // 답변이 하나라도 있을 경우 삭제 불가 처리
        if (answerAdapter.getItemCount() > 0) {
            Toast.makeText(
                    QuestionDetailActivity.this,
                    "답변이 있는 질문은 삭제가 불가능합니다",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // 기존 삭제 로직
        QuestionApi api = RetrofitClient.getClient().create(QuestionApi.class);
        api.deleteQuestion(questionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(
                            QuestionDetailActivity.this,
                            "삭제되었습니다.",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                } else {
                    Toast.makeText(
                            QuestionDetailActivity.this,
                            "삭제 실패: " + response.code(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(
                        QuestionDetailActivity.this,
                        "오류: " + t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAnswerList(questionId);
    }
}
