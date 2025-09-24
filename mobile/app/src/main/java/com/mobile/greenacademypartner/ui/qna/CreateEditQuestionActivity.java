package com.mobile.greenacademypartner.ui.qna;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.QuestionApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Question;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateEditQuestionActivity extends AppCompatActivity {
    private EditText etTitle, etContent;
    private Button btnSave;
    private Spinner spinnerAcademy;
    private int selectedAcademyNumber = -1;
    private String questionId;
    private boolean isEdit;

    // 편집 시 기존 값 보존용
    private Question originalQuestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_edit_question);

        etTitle   = findViewById(R.id.et_question_title);
        etContent = findViewById(R.id.et_question_content);
        spinnerAcademy = findViewById(R.id.spinner_academy);
        btnSave   = findViewById(R.id.btn_save_question);

        // ✅ 내용 입력은 사용 안 함: 숨김 처리
        if (etContent != null) etContent.setVisibility(View.GONE);

        // 질문 수정 여부
        questionId = getIntent().getStringExtra("questionId");
        isEdit     = questionId != null && !questionId.isEmpty();

        // 1) Spinner: 학생이 가진 academyNumbers(JSON 배열) 읽어오기
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String academyArray = prefs.getString("academyNumbers", "[]");
        List<Integer> academyNums = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(academyArray);
            for (int i = 0; i < arr.length(); i++) {
                academyNums.add(arr.getInt(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 문자열 리스트로 변환
        List<String> academyLabels = new ArrayList<>();
        for (Integer num : academyNums) {
            academyLabels.add(String.valueOf(num));
        }

        // 어댑터 설정
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                academyLabels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAcademy.setAdapter(adapter);

        // 선택 이벤트
        spinnerAcademy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < academyNums.size()) {
                    selectedAcademyNumber = academyNums.get(position);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { /* no-op */ }
        });

        // 수정 모드면 기존 데이터 로드 + 학원 변경 불가
        if (isEdit) {
            loadQuestion(questionId);
            spinnerAcademy.setEnabled(false);
        } else {
            // 신규: 기본 선택 보정
            if (selectedAcademyNumber == -1 && !academyNums.isEmpty()) {
                selectedAcademyNumber = academyNums.get(0);
            }
        }

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                etTitle.setError("제목을 입력하세요.");
                etTitle.requestFocus();
                return;
            }

            String auth = getAuthHeader();
            QuestionApi api = RetrofitClient.getClient().create(QuestionApi.class);

            if (isEdit) {
                // ✅ 편집은 기존 필드 보존 + 제목만 변경
                if (originalQuestion == null) return; // 안전장치

                Question q = new Question();
                q.setId(originalQuestion.getId());
                q.setTitle(title);
                // content는 사용하지 않지만, 기존 값 보존
                q.setContent(originalQuestion.getContent());
                q.setAuthor(originalQuestion.getAuthor());
                q.setCreatedAt(originalQuestion.getCreatedAt());
                q.setAcademyNumber(originalQuestion.getAcademyNumber());

                api.updateQuestion(auth, questionId, q).enqueue(new Callback<Question>() {
                    @Override public void onResponse(Call<Question> call, Response<Question> response) {
                        if (response.code() == 403) {
                            Toast.makeText(CreateEditQuestionActivity.this, "접근 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        if (response.isSuccessful()) {
                            finish();
                        } else {
                            Toast.makeText(CreateEditQuestionActivity.this, "수정 실패(" + response.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<Question> call, Throwable t) {
                        Toast.makeText(CreateEditQuestionActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                // ✅ 신규 생성: 제목만 필수, content는 빈문자열로
                if (selectedAcademyNumber == -1) {
                    Toast.makeText(this, "학원을 선택하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Question q = new Question();
                q.setTitle(title);
                q.setContent(""); // 서버에서 내용 미사용
                q.setAcademyNumber(selectedAcademyNumber);

                api.createQuestion(auth, q).enqueue(new Callback<Question>() {
                    @Override public void onResponse(Call<Question> call, Response<Question> response) {
                        if (response.code() == 403) {
                            Toast.makeText(CreateEditQuestionActivity.this, "접근 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        if (response.isSuccessful()) {
                            finish();
                        } else {
                            Toast.makeText(CreateEditQuestionActivity.this, "생성 실패(" + response.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<Question> call, Throwable t) {
                        Toast.makeText(CreateEditQuestionActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void loadQuestion(String id) {
        String auth = getAuthHeader();
        QuestionApi api = RetrofitClient.getClient().create(QuestionApi.class);
        api.getQuestion(auth, id).enqueue(new Callback<Question>() {
            @Override
            public void onResponse(Call<Question> call, Response<Question> response) {
                if (response.code() == 403) {
                    Toast.makeText(CreateEditQuestionActivity.this, "접근 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    originalQuestion = response.body();
                    etTitle.setText(originalQuestion.getTitle());
                    // etContent는 숨김 상태이므로 세팅 필요 없음
                } else {
                    Toast.makeText(CreateEditQuestionActivity.this, "불러오기 실패(" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Question> call, Throwable t) {
                Toast.makeText(CreateEditQuestionActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Authorization 헤더 생성
    private String getAuthHeader() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String token = prefs.getString("jwt", null);
        if (token == null || token.isEmpty()) token = prefs.getString("token", null);
        if (token == null || token.isEmpty()) token = prefs.getString("accessToken", null);
        return (token == null || token.isEmpty()) ? null : "Bearer " + token;
    }
}
