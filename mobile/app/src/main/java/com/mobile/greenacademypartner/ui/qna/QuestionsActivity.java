package com.mobile.greenacademypartner.ui.qna;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.QuestionApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Question;
import com.mobile.greenacademypartner.ui.attendance.AttendanceActivity;
import com.mobile.greenacademypartner.ui.main.MainActivity;
import com.mobile.greenacademypartner.ui.mypage.MyPageActivity;
import com.mobile.greenacademypartner.ui.notice.NoticeActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;
import com.mobile.greenacademypartner.ui.timetable.QRScannerActivity;
import com.mobile.greenacademypartner.ui.timetable.StudentTimetableActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuestionsActivity extends AppCompatActivity {

    private RecyclerView rvQuestions;
    private QuestionsAdapter adapter;
    private final List<Question> cards = new ArrayList<>();
    private QuestionApi questionApi;

    // ✅ 추가: 네비게이션 & 화살표 버튼
    private BottomNavigationView bottomNavigation;
    private ImageButton btnHideNav, btnShowNav;
    private Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        // 🔹 상단 툴바
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ✅ 테마 색 적용
        ThemeColorUtil.applyThemeColor(this, toolbar);

        // 🔹 RecyclerView + 어댑터
        rvQuestions = findViewById(R.id.rv_questions);
        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuestionsAdapter(q -> openAcademyRoom(q.getAcademyNumber(), q.getAcademyName()));
        rvQuestions.setAdapter(adapter);

        // 🔹 스피너 숨김
        Spinner sp = findViewById(R.id.spinner_academy);
        if (sp != null) sp.setVisibility(android.view.View.GONE);

        // 🔹 Retrofit Api 인스턴스
        questionApi = RetrofitClient.getClient().create(QuestionApi.class);

        // 🔹 학원 카드 목록 로컬에서 불러오기
        cards.clear();
        cards.addAll(buildAcademyCardsFromPrefs());
        adapter.submitList(new ArrayList<>(cards));

        // 🔹 각 카드 최신 정보 로딩
        for (Question c : cards) {
            fetchResponderNamesForAcademy(c.getAcademyNumber());
        }

        // 🔹 하단 네비게이션 + 화살표 버튼 연결
        bottomNavigation = findViewById(R.id.bottom_navigation);
        btnHideNav = findViewById(R.id.btn_hide_nav);
        btnShowNav = findViewById(R.id.btn_show_nav);

        if (bottomNavigation != null) {
            // ▼ 버튼 → 네비게이션 숨기기
            btnHideNav.setOnClickListener(v -> {
                bottomNavigation.setVisibility(android.view.View.GONE);
                btnHideNav.setVisibility(android.view.View.GONE);
                btnShowNav.setVisibility(android.view.View.VISIBLE);
            });

            // ▲ 버튼 → 네비게이션 보이기
            btnShowNav.setOnClickListener(v -> {
                bottomNavigation.setVisibility(android.view.View.VISIBLE);
                btnShowNav.setVisibility(android.view.View.GONE);
                btnHideNav.setVisibility(android.view.View.VISIBLE);
            });

            // 네비 메뉴 이동 처리
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                } else if (id == R.id.nav_attendance) {
                    startActivity(new Intent(this, AttendanceActivity.class));
                    return true;
                } else if (id == R.id.nav_qr) {
                    startActivity(new Intent(this, QRScannerActivity.class));
                    return true;
                } else if (id == R.id.nav_timetable) {
                    startActivity(new Intent(this, StudentTimetableActivity.class));
                    return true;
                } else if (id == R.id.nav_my) {
                    startActivity(new Intent(this, MyPageActivity.class));
                    return true;
                }
                return false;
            });
        }
    }

    // ------------------ SharedPreferences ------------------

    private String getAuthHeader() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String token = prefs.getString("jwt", null);
        if (token == null || token.isEmpty()) token = prefs.getString("token", null);
        if (token == null || token.isEmpty()) token = prefs.getString("accessToken", null);
        return (token == null || token.isEmpty()) ? null : "Bearer " + token;
    }

    private String getRole() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", null);
        return role == null ? "" : role;
    }

    @Nullable
    private String getSelectedStudentId() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String sid = prefs.getString("selectedStudentId", null);
        return (sid == null || sid.trim().isEmpty()) ? null : sid.trim();
    }

    private List<Question> buildAcademyCardsFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String json = prefs.getString("academyNumbers", "[]");

        List<Integer> nums = parseAcademyNumbers(json);
        List<Question> list = new ArrayList<>();

        for (int n : nums) {
            Question q = new Question();
            q.setId("academy-" + n);
            q.setAcademyNumber(n);
            q.setAcademyName("학원 " + n);
            list.add(q);
        }
        return list;
    }

    private List<Integer> parseAcademyNumbers(String json) {
        List<Integer> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) out.add(arr.getInt(i));
        } catch (JSONException ignored) {}
        return out;
    }

    // ------------------ 서버 연동 ------------------

    private void fetchResponderNamesForAcademy(int academyNumber) {
        String role = getRole();
        String auth = getAuthHeader();

        if ("parent".equalsIgnoreCase(role)) {
            questionApi.getOrCreateParentRoom(auth, academyNumber).enqueue(new Callback<Question>() {
                @Override
                public void onResponse(Call<Question> call, Response<Question> r) {
                    if (!r.isSuccessful() || r.body() == null) return;
                    Question room = r.body();
                    updateCard(academyNumber, room);
                }
                @Override public void onFailure(Call<Question> call, Throwable t) {}
            });
            return;
        }

        String studentId = getSelectedStudentId();
        questionApi.getOrCreateRoom(auth, academyNumber, studentId).enqueue(new Callback<Question>() {
            @Override
            public void onResponse(Call<Question> call, Response<Question> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                Question room = r.body();
                updateCard(academyNumber, room);
            }
            @Override public void onFailure(Call<Question> call, Throwable t) {}
        });
    }

    private void updateCard(int academyNumber, Question room) {
        int idx = -1;
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getAcademyNumber() == academyNumber) {
                idx = i; break;
            }
        }
        if (idx >= 0) {
            Question old = cards.get(idx);
            Question updated = new Question();
            updated.setId(room.getId());
            updated.setAcademyNumber(old.getAcademyNumber());
            updated.setAcademyName(
                    (room.getAcademyName()!=null && !room.getAcademyName().trim().isEmpty())
                            ? room.getAcademyName().trim()
                            : old.getAcademyName()
            );
            updated.setTeacherNames(room.getTeacherNames());
            updated.setUnreadCount(room.getUnreadCount());
            updated.setRecentResponderNames(room.getRecentResponderNames());
            cards.set(idx, updated);
        }
        adapter.submitList(new ArrayList<>(cards));
    }

    // ------------------ 카드 클릭 시 방 열기 ------------------

    private void openAcademyRoom(int academyNumber, String academyName) {
        String auth = getAuthHeader();
        String role = getRole();
        String studentId = getSelectedStudentId();

        if ("parent".equalsIgnoreCase(role)) {
            // ✅ 학부모 전용 방
            questionApi.getOrCreateParentRoom(auth, academyNumber).enqueue(new Callback<Question>() {
                @Override
                public void onResponse(Call<Question> call, Response<Question> response) {
                    if (!response.isSuccessful() || response.body() == null || response.body().getId() == null) {
                        Toast.makeText(QuestionsActivity.this, "채팅방을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String roomId = response.body().getId();

                    Intent intent = new Intent(QuestionsActivity.this, QuestionDetailActivity.class);
                    intent.putExtra("questionId", roomId);
                    intent.putExtra("academyNumber", academyNumber);
                    intent.putExtra("academyName", academyName);
                    startActivity(intent);
                }

                @Override
                public void onFailure(Call<Question> call, Throwable t) {
                    Toast.makeText(QuestionsActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // ✅ 학생/교사/원장
            questionApi.getOrCreateRoom(auth, academyNumber, studentId).enqueue(new Callback<Question>() {
                @Override
                public void onResponse(Call<Question> call, Response<Question> response) {
                    if (!response.isSuccessful() || response.body() == null || response.body().getId() == null) {
                        Toast.makeText(QuestionsActivity.this, "채팅방을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String roomId = response.body().getId();

                    Intent intent = new Intent(QuestionsActivity.this, QuestionDetailActivity.class);
                    intent.putExtra("questionId", roomId);
                    intent.putExtra("academyNumber", academyNumber);
                    intent.putExtra("academyName", academyName);
                    startActivity(intent);
                }

                @Override
                public void onFailure(Call<Question> call, Throwable t) {
                    Toast.makeText(QuestionsActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ------------------ 생명주기 ------------------

    @Override
    protected void onResume() {
        super.onResume();
        if (cards != null && !cards.isEmpty()) {
            for (Question c : cards) {
                fetchResponderNamesForAcademy(c.getAcademyNumber());
            }
        }
        // ✅ 테마 색 다시 적용
        if (toolbar != null) {
            ThemeColorUtil.applyThemeColor(this, toolbar);
        }
    }
}
