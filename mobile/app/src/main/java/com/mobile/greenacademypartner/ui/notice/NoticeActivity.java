package com.mobile.greenacademypartner.ui.notice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;
import com.mobile.greenacademypartner.model.Notice;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
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
    private NoticeApi api;
    private Spinner spinnerAcademy;
    private List<Integer> userAcademyNumbers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);
        // 1. 뷰 초기화
        drawerLayout = findViewById(R.id.drawer_layout_notice);
        toolbar = findViewById(R.id.toolbar_notice);
        navContainer = findViewById(R.id.nav_container_notice);
        rvNotices = findViewById(R.id.rv_notices);
        progressBar = findViewById(R.id.pb_loading_notices);
        btnAdd = findViewById(R.id.btn_add_notice);
        spinnerAcademy = findViewById(R.id.spinner_academy);

        // 2. 툴바 색상 및 설정
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);

        // 3. 드로어 설정
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        // 4. 사이드 메뉴 생성
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null,4);

        // 5. RecyclerView 설정
        rvNotices.setLayoutManager(new LinearLayoutManager(this));
        api = RetrofitClient.getClient().create(NoticeApi.class);

        // 6. 권한에 따라 공지 추가 버튼 제어
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", "");
        if (!"teacher".equals(role) && !"director".equals(role)) {
            btnAdd.setVisibility(View.GONE);
        }

        String academyArray = prefs.getString("academyNumbers", "[]");
        try {
            JSONArray arr = new JSONArray(academyArray);
            for (int i = 0; i < arr.length(); i++) {
                userAcademyNumbers.add(arr.getInt(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

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
                fetchNotices(userAcademyNumbers.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        // 8. 추가 버튼 클릭 시 작성 화면으로 이동
        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, CreateNoticeActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!userAcademyNumbers.isEmpty()) {
            fetchNotices(userAcademyNumbers.get(spinnerAcademy.getSelectedItemPosition()));
        }
    }

    private void fetchNotices(int academyNumber) {
        progressBar.setVisibility(View.VISIBLE);

        api.getNoticesByAcademy(academyNumber).enqueue(new Callback<List<Notice>>() {
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
