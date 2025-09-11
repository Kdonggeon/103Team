// app/src/main/java/com/mobile/greenacademypartner/ui/qna/QuestionsActivity.java
package com.mobile.greenacademypartner.ui.qna;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.QuestionApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.model.Question;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        // 0) 툴바 + 햄버거 토글
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationMenuHelper.setupMenu(
                this,
                findViewById(R.id.nav_container_questions),
                (DrawerLayout) findViewById(R.id.drawer_layout),
                null,
                3
        );

        // 1) RecyclerView + 어댑터
        rvQuestions = findViewById(R.id.rv_questions);
        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuestionsAdapter(q -> openAcademyRoom(q.getAcademyNumber(), q.getAcademyName()));
        rvQuestions.setAdapter(adapter);

        // 2) 스피너/추가 버튼 숨김 (ID는 유지)
        Spinner sp = findViewById(R.id.spinner_academy);
        if (sp != null) sp.setVisibility(View.GONE);
        View btnAdd = findViewById(R.id.btn_add_question);
        if (btnAdd != null) btnAdd.setVisibility(View.GONE);

        // 3) Retrofit Api 인스턴스
        questionApi = RetrofitClient.getClient().create(QuestionApi.class);

        // 4) 학원 카드 목록 구성(로컬) + 표시
        cards.clear();
        cards.addAll(buildAcademyCardsFromPrefs());
        adapter.submitList(new ArrayList<>(cards));

        // 5) 각 학원 카드의 ‘최근 답변자/미확인’ 비동기 로딩
        for (Question c : cards) {
            fetchResponderNamesForAcademy(c.getAcademyNumber());
        }
    }

    // Authorization 헤더 문자열 생성
    private String getAuthHeader() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String token = prefs.getString("jwt", null);
        if (token == null || token.isEmpty()) token = prefs.getString("token", null);
        if (token == null || token.isEmpty()) token = prefs.getString("accessToken", null);
        return (token == null || token.isEmpty()) ? "" : "Bearer " + token;
    }

    // 역할 조회
    private String getRole() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", null);
        return role == null ? "" : role;
    }

    // 학생 ID 조회(없으면 null 반환)
    @Nullable
    private String getSelectedStudentId() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String sid = prefs.getString("selectedStudentId", null);
        return (sid == null || sid.trim().isEmpty()) ? null : sid.trim();
    }

    // SharedPreferences("login_prefs")의 "academyNumbers"에서 카드 생성
    private List<Question> buildAcademyCardsFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String json = prefs.getString("academyNumbers", "[]");

        List<Integer> nums = parseAcademyNumbers(json);
        List<Question> list = new ArrayList<>();

        for (int n : nums) {
            Question q = new Question();
            q.setId("academy-" + n);          // DiffUtil 식별용 임시 ID
            q.setAcademyNumber(n);
            q.setAcademyName("학원 " + n);     // 서버에서 내려온 정식 이름은 fetch에서 갱신
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

    // 학원 방(room) 정보로 최근 답변자/미확인 집계 로딩 (studentId null 허용)
    private void fetchResponderNamesForAcademy(int academyNumber) {
        String auth = getAuthHeader();
        String studentId = getSelectedStudentId(); // null이면 서버가 세션으로 자동 추론(학생/학부모)

        questionApi.getOrCreateRoom(auth, academyNumber, studentId).enqueue(new Callback<Question>() {
            @Override
            public void onResponse(Call<Question> call, Response<Question> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                Question room = r.body();

                // 기존 객체 수정 금지 → 새 인스턴스로 교체
                int idx = -1;
                for (int i = 0; i < cards.size(); i++) {
                    if (cards.get(i).getAcademyNumber() == academyNumber) { idx = i; break; }
                }

                if (idx >= 0) {
                    Question old = cards.get(idx);
                    Question updated = new Question();
                    updated.setId(old.getId());
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
                } else {
                    Question added = new Question();
                    added.setId("academy-" + academyNumber);
                    added.setAcademyNumber(academyNumber);
                    added.setAcademyName(room.getAcademyName());
                    added.setTeacherNames(room.getTeacherNames());
                    added.setUnreadCount(room.getUnreadCount());
                    added.setRecentResponderNames(room.getRecentResponderNames());
                    cards.add(added);
                }

                adapter.submitList(new ArrayList<>(cards));
            }

            @Override
            public void onFailure(Call<Question> call, Throwable t) {
                // 무시(표시는 로컬 기본 상태 유지)
            }
        });
    }

    // 카드 클릭 시 방 열기
    private void openAcademyRoom(int academyNumber, String academyName) {
        String auth = getAuthHeader();
        String studentId = getSelectedStudentId(); // null이면 서버가 세션으로 자동 추론(학생/학부모)
        String role = getRole();

        // 교사/원장은 학생 미지정이면 서버 호출 전에 바로 입력 유도(400 회피)
        if ((role.equalsIgnoreCase("teacher") || role.equalsIgnoreCase("director"))
                && (studentId == null)) {
            promptStudentIdAndOpen(academyNumber, academyName);
            return;
        }

        questionApi.getOrCreateRoom(auth, academyNumber, studentId).enqueue(new Callback<Question>() {
            @Override
            public void onResponse(Call<Question> call, Response<Question> resp) {
                if (resp.code() == 400) {
                    // 교사 계정인데 학생 미지정 → 입력 받아 저장 후 재시도
                    promptStudentIdAndOpen(academyNumber, academyName);
                    return;
                }
                if (!resp.isSuccessful() || resp.body() == null) {
                    Toast.makeText(QuestionsActivity.this,
                            "채팅방을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Question room = resp.body();
                String roomId = room.getId();

                // 읽음 표시(표시 초기화)
                questionApi.markRead(auth, roomId).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> c2, Response<Void> r2) {
                        int idx = -1;
                        for (int i = 0; i < cards.size(); i++) {
                            if (cards.get(i).getAcademyNumber() == academyNumber) { idx = i; break; }
                        }
                        if (idx >= 0) {
                            Question old = cards.get(idx);
                            Question cleared = new Question();
                            cleared.setId(old.getId());
                            cleared.setAcademyNumber(old.getAcademyNumber());
                            cleared.setAcademyName(old.getAcademyName());
                            cleared.setTeacherNames(old.getTeacherNames());
                            cleared.setUnreadCount(0);
                            cleared.setRecentResponderNames(null);
                            cards.set(idx, cleared);
                            adapter.submitList(new ArrayList<>(cards));
                        }
                    }
                    @Override public void onFailure(Call<Void> c2, Throwable t) { /* 무시 */ }
                });

                // 상세 진입
                Intent intent = new Intent(QuestionsActivity.this, QuestionDetailActivity.class);
                intent.putExtra("questionId", roomId);
                intent.putExtra("academyNumber", academyNumber);
                intent.putExtra("academyName",   academyName);
                // 다중 창 상태에서 중복 쌓임/상태 꼬임 방지
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }

            @Override
            public void onFailure(Call<Question> call, Throwable t) {
                Toast.makeText(QuestionsActivity.this,
                        "네트워크 오류로 채팅방을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 교사용: 학생 ID 입력 다이얼로그 → 저장 후 재시도
    private void promptStudentIdAndOpen(int academyNumber, String academyName) {
        final EditText et = new EditText(this);
        et.setHint("학생 ID를 입력하세요");
        new AlertDialog.Builder(this)
                .setTitle("학생 선택")
                .setView(et)
                .setPositiveButton("확인", (d, w) -> {
                    String sid = et.getText().toString().trim();
                    if (!sid.isEmpty()) {
                        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
                        prefs.edit().putString("selectedStudentId", sid).apply();
                        openAcademyRoom(academyNumber, academyName); // 저장 후 재시도
                    } else {
                        Toast.makeText(this, "학생 ID를 입력하세요.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cards != null && !cards.isEmpty()) {
            for (com.mobile.greenacademypartner.model.Question c : cards) {
                fetchResponderNamesForAcademy(c.getAcademyNumber());
            }
        }
    }
}
