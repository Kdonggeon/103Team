package com.mobile.greenacademypartner.ui.timetable;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarIconUtil;
import com.mobile.greenacademypartner.model.student.Student;
import com.mobile.greenacademypartner.ui.adapter.ChildrenListAdapter;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParentChildrenListActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private ListView listView;
    private FloatingActionButton addButton;
    private ChildrenListAdapter adapter;
    private ParentApi api;
    private String parentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_children_list);

        // 툴바 설정
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("자녀 조회");

        // 드로어 설정
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        ToolbarIconUtil.applyWhiteIcons(toolbar, toggle);

        // 사이드 메뉴 설정
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, 2);
        ThemeColorUtil.applyThemeColor(this, toolbar); // 툴바 색상까지 적용됨

        // 리스트 초기화
        listView = findViewById(R.id.children_list_view);
        addButton = findViewById(R.id.btn_add_child);
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        parentId = prefs.getString("username", null);
        Log.d("ParentChildrenList", "parentId: " + parentId);

        if (parentId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 자녀 추가 버튼 클릭 시 AddChildActivity 실행
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddChildActivity.class);
            intent.putExtra("parentId", parentId);
            startActivityForResult(intent, 1001);
        });

        loadChildren();
    }

    private void loadChildren() {
        api = RetrofitClient.getClient().create(ParentApi.class);
        Call<List<Student>> call = api.getChildrenByParentId(parentId);

        call.enqueue(new Callback<List<Student>>() {
            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter = new ChildrenListAdapter(ParentChildrenListActivity.this, response.body());
                    listView.setAdapter(adapter);
                } else {
                    Toast.makeText(ParentChildrenListActivity.this, "자녀 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Student>> call, Throwable t) {
                Toast.makeText(ParentChildrenListActivity.this, "서버 오류 발생", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            loadChildren(); // 자녀 추가 후 리스트 갱신
        }
    }
}
