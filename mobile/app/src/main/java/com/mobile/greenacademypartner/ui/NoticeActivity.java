package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.NoticeApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Notice;
import com.mobile.greenacademypartner.ui.NoticeListAdapter;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NoticeActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private RecyclerView rvNotices;
    private ProgressBar progressBar;
    private Button btnAdd;
    //api 객체
    private NoticeApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);

        //요소 초기화
        drawerLayout = findViewById(R.id.drawer_layout_notice);
        toolbar = findViewById(R.id.toolbar_notice);
        navContainer = findViewById(R.id.nav_container_notice);

        //툴바 초기화
        ToolbarColorUtil.applyToolbarColor(this,toolbar);
        setSupportActionBar(toolbar);

        //햄버거 메뉴 설정
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        //네비게이션 메뉴 연결
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null);

        //공지사항 목록과 버튼 연결
        rvNotices = findViewById(R.id.rv_notices);
        progressBar = findViewById(R.id.pb_loading_notices);
        btnAdd = findViewById(R.id.btn_add_notice);

        //권한 확인(선생만 공지 작성가능)
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        if (!"teacher".equals(prefs.getString("role", ""))) btnAdd.setVisibility(View.GONE);

        //RecyclerView 설정
        rvNotices.setLayoutManager(new LinearLayoutManager(this));
        api = RetrofitClient.getClient().create(NoticeApi.class);

        //공지사항 목록 요청
        fetchNotices();

        //작성 버튼 클릭시 CreateNoticeActivity로 이동
        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, CreateNoticeActivity.class))
        );

    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchNotices();
    }

    private void fetchNotices() {
        progressBar.setVisibility(View.VISIBLE);
        api.getNotices().enqueue(new Callback<List<Notice>>() {
            @Override
            public void onResponse(Call<List<Notice>> call, Response<List<Notice>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    NoticeListAdapter adapter = new NoticeListAdapter(
                            response.body(),
                            notice -> {
                                Intent it = new Intent(NoticeActivity.this, NoticeDetailActivity.class);
                                it.putExtra("notice_id", notice.getId());
                                startActivity(it);
                            }
                    );
                    rvNotices.setAdapter(adapter);
                } else {
                    Toast.makeText(NoticeActivity.this,
                            "목록 조회 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Notice>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NoticeActivity.this,
                        "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
