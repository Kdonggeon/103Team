package com.mobile.greenacademypartner.ui.qna;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.QuestionApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Question;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuestionsActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private RecyclerView rvQuestions;
    private QuestionsAdapter adapter;
    private ProgressBar pbLoading;
    private TextView tvMessage;
    private Button btnAddQuestion;
    private Spinner spinnerAcademy;

    private List<Integer> userAcademyNumbers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        // 툴바 및 드로어 설정
        drawerLayout = findViewById(R.id.drawer_layout_questions);
        toolbar = findViewById(R.id.toolbar_questions);
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 뷰 바인딩
        spinnerAcademy = findViewById(R.id.spinner_academy);
        btnAddQuestion = findViewById(R.id.btn_add_question);
        pbLoading = findViewById(R.id.pb_loading_questions);
        tvMessage = findViewById(R.id.main_content_text_questions);
        rvQuestions = findViewById(R.id.rv_questions);

        // 질문 등록 버튼 클릭 이벤트
        btnAddQuestion.setOnClickListener(v -> {
            // CreateEditQuestionActivity로 이동
            Intent intent = new Intent(QuestionsActivity.this, CreateEditQuestionActivity.class);
            startActivity(intent);
        });

        // RecyclerView 초기화
        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuestionsAdapter(q -> {
            Intent intent = new Intent(this, QuestionDetailActivity.class);
            intent.putExtra("questionId", q.getId());
            startActivity(intent);
        });
        rvQuestions.setAdapter(adapter);

        // 네비게이션 메뉴 구성
        NavigationMenuHelper.setupMenu(
                this,
                findViewById(R.id.nav_container_questions),
                drawerLayout,
                tvMessage,
                3
        );

        // 로그인 정보 확인 및 Spinner에 학원 번호 로드 (해결 방법 1)
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        Log.d("QuestionsAct", "academyNumbers=" + prefs.getString("academyNumbers", ""));

        String academyArray = prefs.getString("academyNumbers", "[]");
        try {
            JSONArray arr = new JSONArray(academyArray);
            for (int i = 0; i < arr.length(); i++) {
                userAcademyNumbers.add(arr.getInt(i));
            }
        } catch (JSONException e) {
            Log.e("QuestionsAct", "academyNumbers 파싱 오류", e);
        }

        // Spinner 어댑터에 번호 리스트 설정
        List<String> labels = new ArrayList<>();
        for (Integer num : userAcademyNumbers) {
            labels.add(String.valueOf(num));
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAcademy.setAdapter(spinnerAdapter);
        spinnerAcademy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fetchQuestions(userAcademyNumbers.get(position));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchQuestions(int academyNumber) {
        pbLoading.setVisibility(View.VISIBLE);
        tvMessage.setVisibility(View.VISIBLE);
        rvQuestions.setVisibility(View.GONE);

        QuestionApi api = RetrofitClient.getClient().create(QuestionApi.class);
        api.getQuestionsByAcademy(academyNumber).enqueue(new Callback<List<Question>>() {
            @Override
            public void onResponse(Call<List<Question>> call, Response<List<Question>> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    adapter.submitList(response.body());
                    rvQuestions.setVisibility(View.VISIBLE);
                    tvMessage.setVisibility(View.GONE);
                } else {
                    tvMessage.setText("등록된 질문이 없습니다.");
                }
            }
            @Override public void onFailure(Call<List<Question>> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                tvMessage.setText("질문 로드 실패");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!userAcademyNumbers.isEmpty()) {
            fetchQuestions(userAcademyNumbers.get(0));
        }
    }
}
