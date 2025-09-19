package com.mobile.greenacademypartner.ui.qna;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.AnswerApi;
import com.mobile.greenacademypartner.api.FollowUpApi;
import com.mobile.greenacademypartner.api.QuestionApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.Answer;
import com.mobile.greenacademypartner.model.FollowUp;
import com.mobile.greenacademypartner.model.Question;
import com.mobile.greenacademypartner.ui.adapter.ThreadAdapter;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuestionDetailActivity extends AppCompatActivity {

    private Toolbar toolbar;

    // 질문 본문
    private TextView tvTitle, tvAuthor, tvDate, tvContent;

    // 스레드 목록(FollowUp + Answer)
    private RecyclerView rvThread;
    private ThreadAdapter threadAdapter;
    // 즉시 반영용 로컬 리스트(낙관적 추가 보존)
    private final List<ThreadAdapter.Item> threadItems = new ArrayList<>();

    // 하단 입력
    private EditText etMessage;
    private Button btnSend;

    // 기존 버튼(호환)
    private Button btnAddAnswer;     // 숨김 처리
    private Button btnDeleteQuestion;

    // 상태
    private String questionId;
    private String role;
    private String username;

    // ───────────────── 유틸 ─────────────────
    private String normalizeRole(String raw) {
        if (raw == null) return "";
        String r = raw.trim().toUpperCase(Locale.ROOT);
        if (r.contains("TEACHER") || r.contains("교사") || r.contains("선생")) return "TEACHER";
        if (r.contains("PARENT") || r.contains("PARENTS") || r.contains("학부모") || r.contains("부모")) return "PARENT";
        if (r.contains("STUDENT") || r.contains("학생")) return "STUDENT";
        return r;
    }

    private String resolveQuestionId(Intent intent) {
        if (intent == null) return null;
        String[] keys = {"questionId", "id", "question_id", "QUESTION_ID"};
        for (String k : keys) {
            String v = intent.getStringExtra(k);
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    private void toastHttpFail(String prefix, Response<?> res) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(" (").append(res.code()).append(")");
        try {
            ResponseBody eb = res.errorBody();
            if (eb != null) {
                String body = new String(eb.bytes(), StandardCharsets.UTF_8);
                if (!body.isEmpty()) sb.append(" : ").append(body);
            }
        } catch (Exception ignored) {}
        Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
    }

    private String formatMd(String s) {
        if (s == null) return "";
        for (String fmt : Arrays.asList(
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
        )) {
            try {
                Date d = new SimpleDateFormat(fmt, Locale.KOREA).parse(s);
                return new SimpleDateFormat("M월 d일", Locale.KOREA).format(d);
            } catch (ParseException ignored) {}
        }
        return s;
    }

    private Date safeParse(String s) {
        if (s == null) return new Date(0);
        for (String fmt : Arrays.asList(
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
        )) {
            try {
                return new SimpleDateFormat(fmt, Locale.KOREA).parse(s);
            } catch (ParseException ignored) {}
        }
        return new Date(0);
    }

    private String safe(String s) { return s == null ? "" : s; }

    private String nz(String s) { return s == null ? "" : s; }
    private String nzTrim(String s) { return s == null ? "" : s.trim(); }

    private boolean isNotEmpty(String s) { return s != null && !s.trim().isEmpty(); }

    private boolean sameItem(ThreadAdapter.Item a, ThreadAdapter.Item b) {
        if (a == null || b == null) return false;
        if (a.type != b.type) return false;
        // 작성자+내용 기준으로 동일 판단(시간 포맷 차이 허용)
        return nz(a.author).equals(nz(b.author))
                && nzTrim(a.content).equals(nzTrim(b.content));
    }

    private List<ThreadAdapter.Item> mergeServerWithLocal(List<ThreadAdapter.Item> serverList) {
        List<ThreadAdapter.Item> finalList = new ArrayList<>();
        if (serverList != null) finalList.addAll(serverList);

        for (ThreadAdapter.Item local : threadItems) {
            boolean exists = false;
            for (ThreadAdapter.Item s : finalList) {
                if (sameItem(s, local)) { exists = true; break; }
            }
            if (!exists) finalList.add(local);
        }
        Collections.sort(finalList, Comparator.comparing(i -> safeParse(i.createdAt)));
        return finalList;
    }

    // ───────────────── 병합/정렬 ─────────────────
    private List<ThreadAdapter.Item> mergeAsThread(List<FollowUp> flist, List<Answer> alist) {
        List<ThreadAdapter.Item> items = new ArrayList<>();

        if (flist != null) {
            for (FollowUp f : flist) {
                // ✅ 학생이름 > 학부모이름 > author(ID)
                String display = isNotEmpty(f.getStudentName()) ? f.getStudentName()
                        : isNotEmpty(f.getParentName()) ? f.getParentName()
                        : safe(f.getAuthor());

                items.add(new ThreadAdapter.Item(
                        ThreadAdapter.Item.Type.USER_FOLLOWUP,
                        display,
                        f.getContent(),
                        f.getCreatedAt()
                ));
            }
        }

        if (alist != null) {
            for (Answer a : alist) {
                // ✅ 교사이름 > author(ID)
                String display = isNotEmpty(a.getTeacherName()) ? a.getTeacherName()
                        : safe(a.getAuthor());

                items.add(new ThreadAdapter.Item(
                        ThreadAdapter.Item.Type.TEACHER_ANSWER,
                        display,
                        a.getContent(),
                        a.getCreatedAt()
                ));
            }
        }

        Collections.sort(items, Comparator.comparing(i -> safeParse(i.createdAt)));
        return items;
    } // ←←← 메서드 정확히 닫기!

    // ───────────────── 라이프사이클 ─────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        // Toolbar
        toolbar = findViewById(R.id.toolbar_question_detail);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            // 다른 화면과 동일하게 두 유틸 모두 적용
            ToolbarColorUtil.applyToolbarColor(this, toolbar);
            ThemeColorUtil.applyThemeColor(this, toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("질문 상세");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // 세션 정보
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        role = normalizeRole(prefs.getString("role", ""));
        username = prefs.getString("username", "");

        // intent
        Intent intent = getIntent();
        questionId = resolveQuestionId(intent);
        if (questionId == null) {
            Toast.makeText(this, "질문 ID가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 질문 본문 뷰
        tvTitle = findViewById(R.id.tv_question_title);
        tvAuthor = findViewById(R.id.tv_question_author);
        tvDate = findViewById(R.id.tv_question_date);
        tvContent = findViewById(R.id.tv_question_content);

        // 기존 버튼(호환)
        btnAddAnswer = findViewById(R.id.btn_add_answer);
        if (btnAddAnswer != null) btnAddAnswer.setVisibility(View.GONE);
        if (btnDeleteQuestion != null) {
            btnDeleteQuestion.setOnClickListener(v -> deleteQuestion());
        }

        // 하단 입력
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> onClickSendMessage());
        }

        // 스레드 RecyclerView
        rvThread = findViewById(R.id.rv_answers);
        rvThread.setLayoutManager(new LinearLayoutManager(this));
        threadAdapter = new ThreadAdapter();
        rvThread.setAdapter(threadAdapter);

        // 데이터 로드
        loadQuestionDetail();
        loadThread();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (toolbar != null) {
            ToolbarColorUtil.applyToolbarColor(this, toolbar);
            ThemeColorUtil.applyThemeColor(this, toolbar);

            // 제목/아이콘 흰색 유지
            toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this, android.R.color.white));
            tintToolbarNavIconWhite(toolbar);
        }
        loadThread();
    }

    private void tintToolbarNavIconWhite(Toolbar tb) {
        Drawable nav = tb.getNavigationIcon();
        if (nav != null) {
            nav = DrawableCompat.wrap(nav.mutate());
            DrawableCompat.setTint(nav, ContextCompat.getColor(this, android.R.color.white));
            tb.setNavigationIcon(nav);
        }
    }

    // ───────────────── 전송 처리 ─────────────────
    private void onClickSendMessage() {
        if (etMessage == null || btnSend == null) return;

        final String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "내용을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false); // 중복 클릭 방지

        if ("TEACHER".equals(role)) {
            // 교사 → 답변
            Answer a = new Answer();
            a.setContent(content);
            AnswerApi api = RetrofitClient.getClient().create(AnswerApi.class);

            api.createAnswer(questionId, a).enqueue(new Callback<Answer>() {
                @Override public void onResponse(Call<Answer> call, Response<Answer> res) {
                    btnSend.setEnabled(true);
                    if (res.isSuccessful()) {
                        Toast.makeText(QuestionDetailActivity.this, "답변 등록 성공", Toast.LENGTH_SHORT).show();
                        etMessage.setText("");

                        String createdAt = (res.body()!=null && res.body().getCreatedAt()!=null)
                                ? res.body().getCreatedAt()
                                : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(new Date());

                        // ✅ 교사이름 우선, 없으면 로그인 아이디
                        String displayName = (res.body()!=null && !TextUtils.isEmpty(res.body().getTeacherName()))
                                ? res.body().getTeacherName()
                                : (username == null ? "" : username);

                        threadItems.add(new ThreadAdapter.Item(
                                ThreadAdapter.Item.Type.TEACHER_ANSWER,
                                displayName,
                                content,
                                createdAt
                        ));
                        threadAdapter.submit(new ArrayList<>(threadItems));
                        rvThread.post(() -> {
                            if (threadAdapter.getItemCount() > 0) {
                                rvThread.scrollToPosition(threadAdapter.getItemCount() - 1);
                            }
                        });

                        // 서버 목록으로 재동기화
                        loadThread();
                    } else {
                        toastHttpFail("답변 등록 실패", res);
                    }
                }
                @Override public void onFailure(Call<Answer> call, Throwable t) {
                    btnSend.setEnabled(true);
                    Toast.makeText(QuestionDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } else if ("PARENT".equals(role) || "STUDENT".equals(role)) {
            // 학생/학부모 → 후속 질문
            FollowUp fu = new FollowUp();
            fu.setContent(content);
            FollowUpApi api = RetrofitClient.getClient().create(FollowUpApi.class);

            api.create(questionId, fu).enqueue(new Callback<FollowUp>() {
                @Override public void onResponse(Call<FollowUp> call, Response<FollowUp> res) {
                    btnSend.setEnabled(true);
                    if (res.isSuccessful()) {
                        Toast.makeText(QuestionDetailActivity.this, "질문 등록 성공", Toast.LENGTH_SHORT).show();
                        etMessage.setText("");

                        String createdAt = (res.body()!=null && res.body().getCreatedAt()!=null)
                                ? res.body().getCreatedAt()
                                : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(new Date());

                        // ✅ 학생이름 > 학부모이름 > 로그인 아이디
                        String displayName = (res.body()!=null && !TextUtils.isEmpty(res.body().getStudentName())) ? res.body().getStudentName()
                                : (res.body()!=null && !TextUtils.isEmpty(res.body().getParentName())) ? res.body().getParentName()
                                : (username == null ? "" : username);

                        threadItems.add(new ThreadAdapter.Item(
                                ThreadAdapter.Item.Type.USER_FOLLOWUP,
                                displayName,
                                content,
                                createdAt
                        ));
                        threadAdapter.submit(new ArrayList<>(threadItems));
                        rvThread.post(() -> {
                            if (threadAdapter.getItemCount() > 0) {
                                rvThread.scrollToPosition(threadAdapter.getItemCount() - 1);
                            }
                        });

                        // 서버 목록으로 재동기화
                        loadThread();
                    } else {
                        toastHttpFail("질문 등록 실패", res);
                    }
                }
                @Override public void onFailure(Call<FollowUp> call, Throwable t) {
                    btnSend.setEnabled(true);
                    Toast.makeText(QuestionDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } else {
            btnSend.setEnabled(true);
            Toast.makeText(this, "이 역할은 전송 권한이 없습니다. (role=" + role + ")", Toast.LENGTH_LONG).show();
        }
    }

    // ───────────────── 질문 상세 ─────────────────
    private void loadQuestionDetail() {
        QuestionApi api = RetrofitClient.getClient().create(QuestionApi.class);
        api.getQuestion(questionId).enqueue(new Callback<Question>() {
            @Override
            public void onResponse(Call<Question> call, Response<Question> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(QuestionDetailActivity.this, "질문 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Question q = response.body();

                // 제목: "선생1의 답변" / "선생1, 선생2의 답변" / 미답변
                String teachersTitle;
                if (q.getTeacherNames() != null && !q.getTeacherNames().isEmpty()) {
                    List<String> names = q.getTeacherNames();
                    String subject = (names.size() == 1)
                            ? names.get(0)
                            : TextUtils.join(", ", names);
                    teachersTitle = subject + "의 답변";
                } else {
                    teachersTitle = "미답변";
                }

                // 부제: 학원명(없으면 학원번호)
                String academyLabel = (!TextUtils.isEmpty(q.getAcademyName()))
                        ? q.getAcademyName()
                        : ("학원번호: " + q.getAcademyNumber());

                if (tvTitle  != null) tvTitle.setText(teachersTitle);
                if (tvAuthor != null) tvAuthor.setText(academyLabel);

                if (tvDate    != null) tvDate.setVisibility(View.GONE);
                if (tvContent != null) tvContent.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<Question> call, Throwable t) {
                Toast.makeText(QuestionDetailActivity.this, "오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ───────────────── 스레드 로딩 ─────────────────
    private void loadThread() { loadThread(null); }

    private void loadThread(Runnable onComplete) {
        final FollowUpApi fapi = RetrofitClient.getClient().create(FollowUpApi.class);
        final AnswerApi aapi = RetrofitClient.getClient().create(AnswerApi.class);

        fapi.list(questionId).enqueue(new Callback<List<FollowUp>>() {
            @Override
            public void onResponse(Call<List<FollowUp>> call, Response<List<FollowUp>> fRes) {
                final List<FollowUp> fuList = fRes.isSuccessful() && fRes.body() != null
                        ? fRes.body() : Collections.emptyList();

                aapi.listAnswers(questionId).enqueue(new Callback<List<Answer>>() {
                    @Override
                    public void onResponse(Call<List<Answer>> call, Response<List<Answer>> aRes) {
                        List<Answer> aList = aRes.isSuccessful() && aRes.body() != null
                                ? aRes.body() : Collections.emptyList();

                        List<ThreadAdapter.Item> merged = mergeAsThread(fuList, aList);
                        List<ThreadAdapter.Item> finalList = mergeServerWithLocal(merged);

                        threadItems.clear();
                        threadItems.addAll(finalList);
                        threadAdapter.submit(new ArrayList<>(threadItems));

                        rvThread.post(() -> {
                            if (threadAdapter.getItemCount() > 0) {
                                rvThread.scrollToPosition(threadAdapter.getItemCount() - 1);
                            }
                        });

                        if (onComplete != null) onComplete.run();
                    }

                    @Override
                    public void onFailure(Call<List<Answer>> call, Throwable t) {
                        List<ThreadAdapter.Item> merged = mergeAsThread(fuList, Collections.emptyList());
                        List<ThreadAdapter.Item> finalList = mergeServerWithLocal(merged);

                        threadItems.clear();
                        threadItems.addAll(finalList);
                        threadAdapter.submit(new ArrayList<>(threadItems));

                        if (onComplete != null) onComplete.run();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<FollowUp>> call, Throwable t) {
                aapi.listAnswers(questionId).enqueue(new Callback<List<Answer>>() {
                    @Override
                    public void onResponse(Call<List<Answer>> call, Response<List<Answer>> aRes) {
                        List<Answer> aList = aRes.isSuccessful() && aRes.body() != null
                                ? aRes.body() : Collections.emptyList();

                        List<ThreadAdapter.Item> merged = mergeAsThread(Collections.emptyList(), aList);
                        List<ThreadAdapter.Item> finalList = mergeServerWithLocal(merged);

                        threadItems.clear();
                        threadItems.addAll(finalList);
                        threadAdapter.submit(new ArrayList<>(threadItems));

                        rvThread.post(() -> {
                            if (threadAdapter.getItemCount() > 0) {
                                rvThread.scrollToPosition(threadAdapter.getItemCount() - 1);
                            }
                        });

                        if (onComplete != null) onComplete.run();
                    }

                    @Override
                    public void onFailure(Call<List<Answer>> call, Throwable t) {
                        // 양쪽 실패 시 기존 화면 유지(로컬 보존)
                        threadAdapter.submit(new ArrayList<>(threadItems));
                        if (onComplete != null) onComplete.run();
                    }
                });
            }
        });
    }

    // ───────────────── 삭제 ─────────────────
    private void deleteQuestion() {
        QuestionApi api = RetrofitClient.getClient().create(QuestionApi.class);
        api.deleteQuestion(questionId).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(QuestionDetailActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(QuestionDetailActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(QuestionDetailActivity.this, "오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // (레거시 호환) 외부에서 호출하는 시그니처 유지
    public void loadAnswerList(String questionId) {
        if (questionId != null && !questionId.equals(this.questionId)) this.questionId = questionId;
        loadThread();
    }
    public void loadAnswerList(String questionId, Runnable onComplete) {
        if (questionId != null && !questionId.equals(this.questionId)) this.questionId = questionId;
        loadThread(onComplete);
    }
}
