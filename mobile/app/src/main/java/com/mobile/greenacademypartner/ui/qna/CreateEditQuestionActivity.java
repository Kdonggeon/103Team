package com.mobile.greenacademypartner.ui.qna;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_edit_question);

        etTitle   = findViewById(R.id.et_question_title);
        etContent = findViewById(R.id.et_question_content);
        spinnerAcademy = findViewById(R.id.spinner_academy);
        btnSave   = findViewById(R.id.btn_save_question);

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
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAcademyNumber = academyNums.get(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 기본값 유지
            }
        });

        // 수정 모드면 기존 데이터 로드
        if (isEdit) {
            loadQuestion(questionId);
            spinnerAcademy.setEnabled(false); // 수정 시 학원 변경 불가
        }

        btnSave.setOnClickListener(v -> {
            Question q = new Question();
            q.setTitle(etTitle.getText().toString());
            q.setContent(etContent.getText().toString());
            String authorId = prefs.getString("username", "");
            q.setAuthor(authorId);

            // 신규 작성 시 선택된 학원 번호 설정
            if (!isEdit) {
                q.setAcademyNumber(selectedAcademyNumber);
            }

            QuestionApi api = RetrofitClient.getClient().create(QuestionApi.class);
            Call<Question> call = isEdit
                    ? api.updateQuestion(questionId, q)
                    : api.createQuestion(q);
            call.enqueue(new Callback<Question>() {
                @Override
                public void onResponse(Call<Question> call, Response<Question> response) {
                    if (response.isSuccessful()) finish();
                }
                @Override
                public void onFailure(Call<Question> call, Throwable t) {
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
