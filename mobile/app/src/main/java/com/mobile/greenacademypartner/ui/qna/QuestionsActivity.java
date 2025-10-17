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

import java.nio.charset.StandardCharsets;
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

    // Authorization 헤더 문자열 생성 (토큰 없으면 null 반환 → 헤더 생략)
    private String getAuthHeader() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String token = prefs.getString("jwt", null);
        if (token == null || token.isEmpty()) token = prefs.getString("token", null);
        if (token == null || token.isEmpty()) token = prefs.getString("accessToken", null);
        return (token == null || token.isEmpty()) ? null : "Bearer " + token;
    }

    // 역할 조회
    private String getRole() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", null);
        return role == null ? "" : role;
    }

    // 학생 ID 조회(없으면 null 반환) — 교사/원장 흐름에서만 사용
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

    // 학원 방(room) 정보로 최근 답변자/미확인 집계 로딩
    private void fetchResponderNamesForAcademy(int academyNumber) {
        String role = getRole();
        String auth = requireAuthOrWarn();
        if (auth == null) return;

        if ("parent".equalsIgnoreCase(role)) {
            // 학부모 전용 방 요약 갱신
            questionApi.getOrCreateParentRoom(auth, academyNumber).enqueue(new Callback<Question>() {
                @Override
                public void onResponse(Call<Question> call, Response<Question> r) {
                    if (!r.isSuccessful() || r.body() == null) {
                        toastHttpFail("채팅방 요약 갱신 실패", r);
                        return;
                    }
                    Question room = r.body();

                    int idx = -1;
                    for (int i = 0; i < cards.size(); i++) {
                        if (cards.get(i).getAcademyNumber() == academyNumber) { idx = i; break; }
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
                        adapter.submitList(new ArrayList<>(cards));
                    }
                }
                @Override public void onFailure(Call<Question> call, Throwable t) {
                    Toast.makeText(QuestionsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        // 학생/교사/원장
        String studentId = getSelectedStudentId();

        // 요약 갱신 단계에서는 교사/원장에 대해 studentId가 비어있으면 스킵(다이얼로그 난사 방지)
        if ((role.equalsIgnoreCase("teacher") || role.equalsIgnoreCase("director")) && (studentId == null)) {
            return;
        }

        questionApi.getOrCreateRoom(auth, academyNumber, studentId).enqueue(new Callback<Question>() {
            @Override
            public void onResponse(Call<Question> call, Response<Question> r) {
                if (!r.isSuccessful() || r.body() == null) {
                    toastHttpFail("채팅방 요약 갱신 실패", r);
                    return;
                }
                Question room = r.body();

                int idx = -1;
                for (int i = 0; i < cards.size(); i++) {
                    if (cards.get(i).getAcademyNumber() == academyNumber) { idx = i; break; }
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
                Toast.makeText(QuestionsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 카드 클릭 시 방 열기
    private void openAcademyRoom(int academyNumber, String academyName) {
        String role = getRole();
        String auth = requireAuthOrWarn();
        if (auth == null) return;

        if ("parent".equalsIgnoreCase(role)) {
            // ★ 학부모: 학부모 전용 방으로 바로 진입
            questionApi.getOrCreateParentRoom(auth, academyNumber).enqueue(new Callback<Question>() {
                @Override
                public void onResponse(Call<Question> call, Response<Question> resp) {
                    if (!resp.isSuccessful() || resp.body() == null || resp.body().getId() == null) {
                        toastHttpFail("채팅방 불러오기 실패", resp);
                        return;
                    }
                    Question room = resp.body();
                    String roomId = room.getId();

                    // 읽음 초기화
                    questionApi.markRead(auth, roomId).enqueue(new Callback<Void>() {
                        @Override public void onResponse(Call<Void> c2, Response<Void> r2) {
                            clearUnreadBadge(academyNumber);
                        }
                        @Override public void onFailure(Call<Void> c2, Throwable t) { }
                    });

                    Intent intent = new Intent(QuestionsActivity.this, QuestionDetailActivity.class);
                    intent.putExtra("questionId", roomId);
                    intent.putExtra("academyNumber", academyNumber);
                    intent.putExtra("academyName",   academyName);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                @Override
                public void onFailure(Call<Question> call, Throwable t) {
                    Toast.makeText(QuestionsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        // 학생/교사/원장: 기존 로직 + 보강
        String studentId = getSelectedStudentId();

        if ((role.equalsIgnoreCase("teacher") || role.equalsIgnoreCase("director")) && (studentId == null)) {
            promptStudentIdAndOpen(academyNumber, academyName);
            return;
        }

        questionApi.getOrCreateRoom(auth, academyNumber, studentId).enqueue(new Callback<Question>() {
            @Override
            public void onResponse(Call<Question> call, Response<Question> resp) {
                if (resp.code() == 400) {
                    if (role.equalsIgnoreCase("teacher") || role.equalsIgnoreCase("director")) {
                        promptStudentIdAndOpen(academyNumber, academyName);
                        return;
                    }
                }
                if (!resp.isSuccessful() || resp.body() == null) {
                    toastHttpFail("채팅방 불러오기 실패", resp);
                    return;
                }
                Question room = resp.body();
                String roomId = room.getId();

                // 읽음 표시(표시 초기화)
                questionApi.markRead(auth, roomId).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> c2, Response<Void> r2) {
                        clearUnreadBadge(academyNumber);
                    }
                    @Override public void onFailure(Call<Void> c2, Throwable t) { }
                });

                // 상세 진입
                Intent intent = new Intent(QuestionsActivity.this, QuestionDetailActivity.class);
                intent.putExtra("questionId", roomId);
                intent.putExtra("academyNumber", academyNumber);
                intent.putExtra("academyName",   academyName);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }

            @Override
            public void onFailure(Call<Question> call, Throwable t) {
                Toast.makeText(QuestionsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 읽음 배지 초기화(로컬 카드 갱신)
    private void clearUnreadBadge(int academyNumber) {
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

    // 교사/원장: 학생 ID 입력 다이얼로그 → 저장 후 재시도
    private void promptStudentIdAndOpen(int academyNumber, String academyName) {
        final EditText et = new EditText(this);
        et.setHint("ID를 입력하세요");

        new AlertDialog.Builder(this)
                .setTitle("채팅 대상 ID")
                .setView(et)
                .setPositiveButton("확인", (d, w) -> {
                    String entered = et.getText().toString().trim();
                    if (entered.isEmpty()) {
                        Toast.makeText(this, "ID를 입력하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String auth = requireAuthOrWarn();
                    if (auth == null) return;

                    QuestionApi questionApi = RetrofitClient.getClient().create(QuestionApi.class);

                    questionApi.getOrCreateRoomById(auth, academyNumber, entered)
                            .enqueue(new Callback<Question>() {
                                @Override
                                public void onResponse(Call<Question> call, Response<Question> resp) {
                                    if (!resp.isSuccessful() || resp.body() == null || resp.body().getId() == null) {
                                        toastHttpFail("채팅방 불러오기 실패", resp);
                                        return;
                                    }
                                    Question room = resp.body();

                                    // 읽음 처리(선택)
                                    questionApi.markRead(auth, room.getId()).enqueue(new Callback<Void>() {
                                        @Override public void onResponse(Call<Void> c2, Response<Void> r2) {
                                            clearUnreadBadge(academyNumber);
                                        }
                                        @Override public void onFailure(Call<Void> c2, Throwable t) { }
                                    });

                                    Intent intent = new Intent(QuestionsActivity.this, QuestionDetailActivity.class);
                                    intent.putExtra("questionId", room.getId());
                                    intent.putExtra("academyNumber", academyNumber);
                                    intent.putExtra("academyName", academyName);
                                    startActivity(intent);
                                }

                                @Override
                                public void onFailure(Call<Question> call, Throwable t) {
                                    Toast.makeText(QuestionsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cards != null && !cards.isEmpty()) {
            for (Question c : cards) {
                fetchResponderNamesForAcademy(c.getAcademyNumber());
            }
        }
    }

    // ---- 보조 메서드 (실패 가시화/인증 보장) ---------------------------------

    private void toastHttpFail(String prefix, Response<?> res) {
        String msg = prefix + " (" + res.code() + ")";
        try {
            if (res.errorBody() != null) {
                String body = new String(res.errorBody().bytes(), StandardCharsets.UTF_8);
                if (!body.isEmpty()) msg += "\n" + body;
            }
        } catch (Exception ignored) {}
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Nullable
    private String requireAuthOrWarn() {
        String auth = getAuthHeader();
        if (auth == null) {
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show();
            return null;
        }
        return auth;
    }
}
