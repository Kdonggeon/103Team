package com.mobile.greenacademypartner.ui.notice;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.NoticeApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Notice;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateNoticeActivity extends AppCompatActivity {
    private EditText etTitle, etContent;
    private Button btnSubmit;
    private Spinner spinnerAcademy;

    private NoticeApi api;
    private List<Integer> academyNumbers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_create_notice);

        // UI 요소 초기화
        etTitle = findViewById(R.id.edit_notice_title);
        etContent = findViewById(R.id.edit_notice_content);
        btnSubmit = findViewById(R.id.btn_notice_submit);
        spinnerAcademy = findViewById(R.id.spinner_academy);  // ✅ Spinner 추가

        // Retrofit API 준비
        api = RetrofitClient.getClient().create(NoticeApi.class);

        // SharedPreferences에서 academyNumbers 불러오기
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String academyStr = prefs.getString("academyNumbers", "[]");

        try {
            JSONArray arr = new JSONArray(academyStr);
            for (int i = 0; i < arr.length(); i++) {
                academyNumbers.add(arr.getInt(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 학원번호 Spinner 초기화
        if (!academyNumbers.isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (int num : academyNumbers) {
                labels.add(String.valueOf(num));
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, labels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerAcademy.setAdapter(adapter);
        } else {
            Toast.makeText(this, "연결된 학원이 없습니다.", Toast.LENGTH_LONG).show();
            btnSubmit.setEnabled(false);
        }

        // 제출 버튼 클릭 시
        btnSubmit.setOnClickListener(v -> {
            String t = etTitle.getText().toString().trim();
            String c = etContent.getText().toString().trim();

            if (t.isEmpty() || c.isEmpty()) {
                Toast.makeText(this, "제목/내용 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            Notice notice = new Notice();
            notice.setTitle(t);
            notice.setContent(c);

            String name = prefs.getString("name", "");
            notice.setAuthor(name);

            // ✅ 선택된 학원번호로 설정
            int selectedAcademy = academyNumbers.get(spinnerAcademy.getSelectedItemPosition());
            notice.setAcademyNumber(selectedAcademy);

            api.createNotice(notice).enqueue(new Callback<Notice>() {
                @Override
                public void onResponse(Call<Notice> call, Response<Notice> r) {
                    if (r.isSuccessful()) {
                        Toast.makeText(CreateNoticeActivity.this, "등록 성공!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(CreateNoticeActivity.this, "실패: " + r.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Notice> call, Throwable t) {
                    Toast.makeText(CreateNoticeActivity.this, "오류", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
