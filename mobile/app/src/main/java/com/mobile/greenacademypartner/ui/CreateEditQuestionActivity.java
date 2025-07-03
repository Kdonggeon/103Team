package com.mobile.greenacademypartner.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.QuestionApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Question;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateEditQuestionActivity extends AppCompatActivity {
    private EditText etTitle, etContent;
    private Button btnSave;
    private String questionId;
    private boolean isEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_edit_question);

        etTitle   = findViewById(R.id.et_question_title);
        etContent = findViewById(R.id.et_question_content);
        btnSave   = findViewById(R.id.btn_save_question);

        questionId = getIntent().getStringExtra("questionId");
        isEdit     = questionId != null;

        if (isEdit) loadQuestion(questionId);

        btnSave.setOnClickListener(v -> {
            Question q = new Question();
            q.setTitle(etTitle.getText().toString());
            q.setContent(etContent.getText().toString());
            // 현재 사용자 ID SharedPreferences에서 로드
            String authorId = getSharedPreferences("userPrefs", MODE_PRIVATE)
                    .getString("userId", "");
            q.setAuthor(authorId);
            QuestionApi api = RetrofitClient.getClient().create(QuestionApi.class);
            Call<Question> call = isEdit
                    ? api.updateQuestion(questionId, q)
                    : api.createQuestion(q);
            call.enqueue(new Callback<Question>() {
                @Override public void onResponse(Call<Question> call, Response<Question> response) {
                    if (response.isSuccessful()) finish();
                }
                @Override public void onFailure(Call<Question> call, Throwable t) {
                    // TODO: 에러 처리
                }
            });
        });
    }

    private void loadQuestion(String id) {
        QuestionApi api = RetrofitClient.getClient().create(QuestionApi.class);
        api.getQuestion(id).enqueue(new Callback<Question>() {
            @Override
            public void onResponse(Call<Question> call, Response<Question> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Question q = response.body();
                    etTitle.setText(q.getTitle());
                    etContent.setText(q.getContent());
                }
            }
            @Override public void onFailure(Call<Question> call, Throwable t) {
                // TODO: 에러 처리
            }
        });
    }
}