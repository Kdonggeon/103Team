package com.mobile.greenacademypartner.ui.notification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.NotificationApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.ui.main.MainActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView rvNotifications;
    private View emptyState;
    private MaterialButton btnClearAll;

    private NotificationAdapter adapter;
    private final List<NotificationItem> items = new ArrayList<>();
    private int page = 1;
    private final int pageSize = 20;
    private boolean isLoading = false;
    private String userId; // 로그인 prefs에서 꺼냄

    // 설정에서 저장한 테마 색상
    private int themeColor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // View binding
        toolbar = findViewById(R.id.toolbar);
        rvNotifications = findViewById(R.id.rvNotifications);
        emptyState = findViewById(R.id.emptyState);
        btnClearAll = findViewById(R.id.btnClearAll);

        // 테마 적용
        loadAndApplyThemeColor();

        // Toolbar 설정
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // 액션바 기본 타이틀 숨김
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // 툴바 자체 타이틀도 비워 중복 방지
        toolbar.setTitle("");

        // 네비게이션 아이콘(한 개만 사용)
        try {
            toolbar.setNavigationIcon(R.drawable.outline_chevron_left_24);
        } catch (Throwable ignore) {
            toolbar.setNavigationIcon(android.R.drawable.ic_media_previous);
        }
        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // RecyclerView
        adapter = new NotificationAdapter(items);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        // 로그인 정보에서 userId
        SharedPreferences login = getSharedPreferences("login_prefs", MODE_PRIVATE);
        userId = login.getString("userId", login.getString("username", ""));

        // 첫 로드
        fetchNotifications(true);

        // 무한 스크롤
        rvNotifications.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int total = adapter.getItemCount();
                int last = lm.findLastVisibleItemPosition();
                if (!isLoading && last >= total - 3) {
                    fetchNotifications(false);
                }
            }
        });

        // 전체 삭제
        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> {
                if (items.isEmpty()) {
                    Toast.makeText(this, "삭제할 알림이 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle("전체 삭제")
                        .setMessage("모든 알림을 삭제할까요?")
                        .setPositiveButton("삭제", (d, w) -> performDeleteAll())
                        .setNegativeButton("취소", null)
                        .show();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAndApplyThemeColor();
    }

    /** 설정의 테마 색상 불러와서 적용 */
    private void loadAndApplyThemeColor() {
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        int defaultColor = ContextCompat.getColor(this, R.color.green);
        themeColor = settings.getInt("theme_color", defaultColor);

        if (toolbar != null) toolbar.setBackgroundColor(themeColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(themeColor);
        }
        if (btnClearAll != null) {
            btnClearAll.setBackgroundTintList(ColorStateList.valueOf(themeColor));
        }
    }

    /** 빈/리스트 토글 */
    private void toggleEmpty(boolean showEmpty) {
        if (rvNotifications == null || emptyState == null) return;
        rvNotifications.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
    }

    /** 알림 목록 로드 */
    private void fetchNotifications(boolean reset) {
        if (isLoading) return;
        isLoading = true;
        if (reset) {
            page = 1;
            items.clear();
            adapter.notifyDataSetChanged();
            toggleEmpty(false);
        }

        NotificationApi api = RetrofitClient.getClient().create(NotificationApi.class);
        api.getNotifications(userId, page, pageSize).enqueue(new Callback<List<NotificationItem>>() {
            @Override
            public void onResponse(Call<List<NotificationItem>> call, Response<List<NotificationItem>> res) {
                isLoading = false;
                if (!res.isSuccessful() || res.body() == null) {
                    toggleEmpty(items.isEmpty());
                    Toast.makeText(NotificationActivity.this, "알림 불러오기 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<NotificationItem> newItems = res.body();
                int start = items.size();
                items.addAll(newItems);
                adapter.notifyItemRangeInserted(start, newItems.size());
                toggleEmpty(items.isEmpty());
                if (!newItems.isEmpty()) page++;
            }

            @Override
            public void onFailure(Call<List<NotificationItem>> call, Throwable t) {
                isLoading = false;
                toggleEmpty(items.isEmpty());
                Toast.makeText(NotificationActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 전체 삭제: 서버 요청(있으면) + 로컬 비우기 */
    private void performDeleteAll() {
        NotificationApi api = RetrofitClient.getClient().create(NotificationApi.class);
        api.deleteAll(userId).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> res) {
                if (!res.isSuccessful()) {
                    Toast.makeText(NotificationActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                    return;
                }
                clearLocalAndShowEmpty();
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(NotificationActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });

        // 서버 API가 아직 없다면 아래 한 줄만 호출해도 동작합니다.
        // clearLocalAndShowEmpty();
    }

    /** 로컬 목록 비우고 빈 상태 표시 */
    private void clearLocalAndShowEmpty() {
        int n = items.size();
        items.clear();
        if (n > 0) {
            adapter.notifyItemRangeRemoved(0, n);
        } else {
            adapter.notifyDataSetChanged();
        }
        toggleEmpty(true);
        Toast.makeText(this, "모든 알림을 삭제했습니다.", Toast.LENGTH_SHORT).show();
    }
}
