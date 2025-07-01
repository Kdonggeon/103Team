package com.mobile.greenacademypartner.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.AnswerApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Answer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditAnswerActivity extends AppCompatActivity {
    private EditText etContent;
    private Button btnSave;
    private String answerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_answer);

        etContent = findViewById(R.id.et_edit_answer_content);
        btnSave   = findViewById(R.id.btn_save_answer);

        answerId = getIntent().getStringExtra("answerId");

        loadAnswer(answerId);

        btnSave.setOnClickListener(v -> {
            // 입력된 답변 내용
            String content = etContent.getText().toString();
            // 현재 교사 ID SharedPreferences에서 로드
            String authorId = getSharedPreferences("userPrefs", MODE_PRIVATE)
                    .getString("userId", "");

            Answer a = new Answer();
            a.setContent(content);
            a.setAuthor(authorId);

            AnswerApi api = RetrofitClient.getInstance().create(AnswerApi.class);
            api.updateAnswer(answerId, a).enqueue(new Callback<Answer>() {
                @Override
                public void onResponse(Call<Answer> call, Response<Answer> response) {
                    if (response.isSuccessful()) {
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<Answer> call, Throwable t) {
                    // TODO: 에러 처리
                }
            });
        });
    }

    private void loadAnswer(String id) {
        AnswerApi api = RetrofitClient.getInstance().create(AnswerApi.class);
        api.getAnswer(id).enqueue(new Callback<Answer>() {
            @Override
            public void onResponse(Call<Answer> call, Response<Answer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    etContent.setText(response.body().getContent());
                }
            }

            @Override
            public void onFailure(Call<Answer> call, Throwable t) {
                // TODO: 에러 처리
            }
        });
    }
}
