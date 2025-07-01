package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        // 네비게이션 메뉴 설정
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

        // 버튼 뷰 바인딩
        btnAddQuestion = findViewById(R.id.btn_add_question);
        btnAddQuestion.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateEditQuestionActivity.class));
        });

        // 로딩 및 메시지 뷰
        pbLoading = findViewById(R.id.pb_loading_questions);
        tvMessage = findViewById(R.id.main_content_text_questions);

        // RecyclerView 설정
        rvQuestions = findViewById(R.id.rv_questions);
        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuestionsAdapter(q -> {
            Intent intent = new Intent(this, QuestionDetailActivity.class);
            intent.putExtra("questionId", q.getId());
            startActivity(intent);
        });
        rvQuestions.setAdapter(adapter);

        // NavigationMenu 연결
        // NavigationMenu 연결 (타이틀을 tvMessage에 표시)
        NavigationMenuHelper.setupMenu(
                this,
                findViewById(R.id.nav_container_questions),
                drawerLayout,
                tvMessage
        );

        // 데이터 로딩
        fetchQuestions();
    }

    private void fetchQuestions() {
        pbLoading.setVisibility(View.VISIBLE);
        tvMessage.setVisibility(View.VISIBLE);
        rvQuestions.setVisibility(View.GONE);

        QuestionApi api = RetrofitClient.getInstance().create(QuestionApi.class);
        api.listQuestions().enqueue(new Callback<List<Question>>() {
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
            @Override
            public void onFailure(Call<List<Question>> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                tvMessage.setText("질문 로드 실패");
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        fetchQuestions();
    }
}
