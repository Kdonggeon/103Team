package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.QaApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Qa;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QAActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private RecyclerView rvQaList;
    private Button btnAdd;
    private QaApi api;
    private QaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qa);

        drawerLayout = findViewById(R.id.drawer_layout_qa);
        toolbar = findViewById(R.id.toolbar_qa);
        navContainer = findViewById(R.id.nav_container_qa);

        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null);

        rvQaList = findViewById(R.id.rv_qa_list);
        btnAdd = findViewById(R.id.btn_add_notice);

        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", "");
        if (!"student".equals(role) && !"parent".equals(role)) {
            btnAdd.setVisibility(View.GONE);
        }

        rvQaList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QaAdapter();
        rvQaList.setAdapter(adapter);
        adapter.setOnItemClickListener(qa -> {
            Intent intent = new Intent(QAActivity.this, QaDetailActivity.class);
            intent.putExtra("qa_id", qa.getId());
            intent.putExtra("qa_title", qa.getTitle());
            intent.putExtra("qa_content", qa.getContent());
            startActivity(intent);
        });

        api = RetrofitClient.getClient().create(QaApi.class);
        fetchQaList();

        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, QaEditActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchQaList();
    }

    private void fetchQaList() {
        api.getAllQa().enqueue(new Callback<List<Qa>>() {
            @Override
            public void onResponse(Call<List<Qa>> call, Response<List<Qa>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setItems(response.body());
                } else {
                    Toast.makeText(QAActivity.this,
                            "목록 조회 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Qa>> call, Throwable t) {
                Toast.makeText(QAActivity.this,
                        "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
