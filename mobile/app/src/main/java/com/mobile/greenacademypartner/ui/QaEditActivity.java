package com.mobile.greenacademypartner.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.QaApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Qa;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QaEditActivity extends AppCompatActivity {
    private EditText etTitle, etContent;
    private Button btnSave;
    private QaApi qaApi;
    private String userId, userRole;
    private String qaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qa_edit);

        Toolbar toolbar = findViewById(R.id.toolbar_qa_edit);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etTitle   = findViewById(R.id.etQaTitle);
        etContent = findViewById(R.id.etQaContent);
        btnSave   = findViewById(R.id.btnSaveQa);

        qaApi = RetrofitClient.getClient().create(QaApi.class);
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        userId   = prefs.getString("username", "");
        userRole = prefs.getString("role", "");

        qaId = getIntent().getStringExtra("qaId");
        if (qaId != null) {
            // 수정 모드: 기존 데이터 로드
            qaApi.getQaById(qaId).enqueue(new Callback<Qa>() {
                @Override
                public void onResponse(Call<Qa> call, Response<Qa> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Qa q = response.body();
                        etTitle.setText(q.getTitle());
                        etContent.setText(q.getContent());
                    }
                }
                @Override
                public void onFailure(Call<Qa> call, Throwable t) {}
            });
        }

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            Qa q = new Qa();
            q.setTitle(title);
            q.setContent(content);
            q.setAuthor(userId);
            q.setAuthorRole(userRole);

            if (qaId == null) {
                // 새 질문 작성
                qaApi.createQa(q).enqueue(new Callback<Qa>() {
                    @Override
                    public void onResponse(Call<Qa> call, Response<Qa> response) {
                        if (response.isSuccessful()) finish();
                    }
                    @Override
                    public void onFailure(Call<Qa> call, Throwable t) {}
                });
            } else {
                // 질문 수정
                qaApi.updateQa(qaId, q).enqueue(new Callback<Qa>() {
                    @Override
                    public void onResponse(Call<Qa> call, Response<Qa> response) {
                        if (response.isSuccessful()) finish();
                    }
                    @Override
                    public void onFailure(Call<Qa> call, Throwable t) {}
                });
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
