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
        setContentView(R.layout.activity_question_detail);

        //  Toolbar 설정
        toolbar = findViewById(R.id.toolbar_question_detail);
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //  상단 네비게이션
        navContainer = findViewById(R.id.nav_container_question_detail);
        mainContentText = findViewById(R.id.main_content_text_question_detail);

        //  버튼
        btnDeleteQuestion = findViewById(R.id.btn_delete_question);
        btnAddAnswer = findViewById(R.id.btn_add_answer);

        //  질문 상세 정보 뷰
        tvTitle = findViewById(R.id.tv_question_title);
        tvContent = findViewById(R.id.tv_question_content);
        tvAuthor = findViewById(R.id.tv_question_author);
        tvDate = findViewById(R.id.tv_question_date);

        //  RecyclerView 세팅
        rvAnswers = findViewById(R.id.rv_answers);
        rvAnswers.setLayoutManager(new LinearLayoutManager(this));

        //  로그인 사용자 ID 가져오기
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        userRole = prefs.getString("role", "");

        //  QuestionId 전달받기
        questionId = getIntent().getStringExtra("questionId");
        if (questionId == null || questionId.isEmpty()) {
            finish();
            return;
        }

        //  AnswerAdapter 생성
        answerAdapter = new AnswerAdapter(this, userRole, questionId);
        rvAnswers.setAdapter(answerAdapter);

        //  질문/답변 불러오기
        loadQuestionDetail(questionId);
        loadAnswerList(questionId);

        //  질문 삭제 버튼
        btnDeleteQuestion.setOnClickListener(v -> deleteQuestion());

        //  답변 작성 버튼
        btnAddAnswer.setOnClickListener(v -> {
            Intent intent = new Intent(QuestionDetailActivity.this, EditAnswerActivity.class);
            intent.putExtra("questionId", questionId);
            startActivity(intent);
        });

        //  교사만 답변 작성 버튼 보이기
        String userRole = prefs.getString("role", "");
        if (!"TEACHER".equalsIgnoreCase(userRole)) {
            btnAddAnswer.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    //  질문 상세 불러오기
    private void loadQuestionDetail(String id) {
        QuestionApi api = RetrofitClient.getClient().create(QuestionApi.class);
        api.getQuestion(id).enqueue(new Callback<Question>() {
            @Override
            public void onResponse(Call<Question> call, Response<Question> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Question q = response.body();

                    tvTitle.setText(q.getTitle());
                    tvContent.setText(q.getContent());
                    tvAuthor.setText(q.getAuthor());

                    if (q.getCreatedAt() != null && q.getCreatedAt().contains("T")) {
                        String date = q.getCreatedAt().split("T")[0];
                        tvDate.setText(date);
                    } else {
                        tvDate.setText(q.getCreatedAt());
                    }

                    mainContentText.setVisibility(View.GONE);
                } else {
                    mainContentText.setText("질문을 불러오지 못했습니다.");
                }
            }

            @Override
            public void onFailure(Call<Question> call, Throwable t) {
                mainContentText.setText("질문을 불러오지 못했습니다.");
            }
        });
    }

    //  답변 목록 불러오기
    public void loadAnswerList() {
        loadAnswerList(questionId);
    }

    private void loadAnswerList(String questionId) {
        AnswerApi api = RetrofitClient.getClient().create(AnswerApi.class);
        api.listAnswers(questionId).enqueue(new Callback<List<Answer>>() {
            @Override
            public void onResponse(Call<List<Answer>> call, Response<List<Answer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    answerAdapter.submitList(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Answer>> call, Throwable t) {
                // 실패 시 로그만
            }
        });
    }

    //  질문 삭제
    private void deleteQuestion() {
        QuestionApi api = RetrofitClient.getClient().create(QuestionApi.class);
        api.deleteQuestion(questionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(QuestionDetailActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(QuestionDetailActivity.this, "삭제 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(QuestionDetailActivity.this, "오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAnswerList(questionId);
    }
}
