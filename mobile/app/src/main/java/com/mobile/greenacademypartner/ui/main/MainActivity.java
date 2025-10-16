package com.mobile.greenacademypartner.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.AnswerApi;
import com.mobile.greenacademypartner.api.NoticeApi;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.model.Answer;
import com.mobile.greenacademypartner.model.Notice;
import com.mobile.greenacademypartner.model.attendance.AttendanceResponse;
import com.mobile.greenacademypartner.model.student.Student;
import com.mobile.greenacademypartner.ui.attendance.AttendanceActivity;
import com.mobile.greenacademypartner.ui.mypage.MyPageActivity;
import com.mobile.greenacademypartner.ui.notice.NoticeActivity;
import com.mobile.greenacademypartner.ui.notice.NoticeDetailActivity;
import com.mobile.greenacademypartner.ui.qna.QuestionDetailActivity;
import com.mobile.greenacademypartner.ui.qna.QuestionsActivity;
import com.mobile.greenacademypartner.ui.setting.SettingActivity;
import com.mobile.greenacademypartner.ui.timetable.QRScannerActivity;
import com.mobile.greenacademypartner.ui.timetable.StudentTimetableActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mobile.greenacademypartner.util.SessionUtil.PREFS_NAME;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private SharedPreferences prefs;

    private TextView tvSelectedChild;
    private ImageView btnAttendance;

    // QnA
    private TextView tvQna1, tvQna2;
    private LinearLayout blockQna1, blockQna2;
    private final List<Answer> recentAnswers = new ArrayList<>();

    // 공지사항
    private TextView tvNotice1, tvNotice2, tvNotice3, tvNotice4;
    private LinearLayout blockNotice1, blockNotice2, blockNotice3, blockNotice4;
    private final List<Notice> recentNotices = new ArrayList<>();

    // 출결 통계
    private TextView tvAttend, tvLate, tvAbsent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // View
        tvSelectedChild = findViewById(R.id.tvSelectedChild);
        btnAttendance = findViewById(R.id.btnAttendance);

        // QnA
        tvQna1 = findViewById(R.id.tvQna1);
        tvQna2 = findViewById(R.id.tvQna2);
        blockQna1 = findViewById(R.id.blockQna1);
        blockQna2 = findViewById(R.id.blockQna2);

        // 공지사항
        tvNotice1 = findViewById(R.id.tvNotice1);
        tvNotice2 = findViewById(R.id.tvNotice2);
        tvNotice3 = findViewById(R.id.tvNotice3);
        tvNotice4 = findViewById(R.id.tvNotice4);
        blockNotice1 = findViewById(R.id.blockNotice1);
        blockNotice2 = findViewById(R.id.blockNotice2);
        blockNotice3 = findViewById(R.id.blockNotice3);
        blockNotice4 = findViewById(R.id.blockNotice4);

        // 출결
        tvAttend = findViewById(R.id.tvAttend);
        tvLate = findViewById(R.id.tvLate);
        tvAbsent = findViewById(R.id.tvAbsent);

        // 🔹 상단 "바로가기 버튼"
        ImageView btnMoreQna = findViewById(R.id.btnMoreQna);
        if (btnMoreQna != null) {
            btnMoreQna.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, QuestionsActivity.class)));
        }

        ImageView btnMoreNotice = findViewById(R.id.btnMoreNotice);
        if (btnMoreNotice != null) {
            btnMoreNotice.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, NoticeActivity.class)));
        }

        // 🔹 QnA 블록 상세보기 이동
        blockQna1.setOnClickListener(v -> openQnaDetailAtIndex(0));
        blockQna2.setOnClickListener(v -> openQnaDetailAtIndex(1));

        // 로그인 정보 확인
        String role = prefs.getString("role", "");
        String studentName = prefs.getString("student_name", "");
        if ("student".equalsIgnoreCase(role)) {
            if (!studentName.isEmpty()) {
                tvSelectedChild.setText(studentName + " 님");
            }
            // 🔹 학생 계정이면 → 화살표(버튼) 숨기고 동작 제거
            if (btnAttendance != null) {
                btnAttendance.setVisibility(View.GONE);
                btnAttendance.setOnClickListener(null);
            }
        } else if ("parent".equalsIgnoreCase(role)) {
            String selectedChild = prefs.getString("selected_child", "");
            if (!selectedChild.isEmpty()) {
                tvSelectedChild.setText(selectedChild + " 님");
            }
            // 🔹 부모 계정일 때만 클릭 시 자녀 선택
            if (btnAttendance != null) {
                btnAttendance.setVisibility(View.VISIBLE);
                btnAttendance.setOnClickListener(v -> handleAttendanceClick());
            }
        }

        // 상단 버튼
        ImageView btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, SettingActivity.class)));
        }
        ImageView btnNotifications = findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, com.mobile.greenacademypartner.ui.notification.NotificationActivity.class)));
        }

        // 하단 네비
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    return true;
                } else if (itemId == R.id.nav_attendance) {
                    startActivity(new Intent(MainActivity.this, AttendanceActivity.class));
                    return true;
                } else if (itemId == R.id.nav_qr) {
                        startActivity(new Intent(this, QRScannerActivity.class));
                    return true;
                } else if (itemId == R.id.nav_timetable) {
                    startActivity(new Intent(MainActivity.this, StudentTimetableActivity.class));
                    return true;
                } else if (itemId == R.id.nav_my) {
                    startActivity(new Intent(MainActivity.this, MyPageActivity.class));
                }
                return true;
            });
        }

        // 서버 데이터
        loadRecentQna();
        loadRecentNoticesAll();
        refreshCurrentAttendance();
    }

    /** 🔹 최근 공지 (전체 학원에서 최신 4개) */
    private void loadRecentNoticesAll() {
        NoticeApi api = RetrofitClient.getClient().create(NoticeApi.class);
        api.listNotices().enqueue(new Callback<List<Notice>>() {
            @Override
            public void onResponse(Call<List<Notice>> call, Response<List<Notice>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Notice> allNotices = response.body();
                    recentNotices.clear();

                    Collections.sort(allNotices, (n1, n2) -> {
                        try {
                            SimpleDateFormat sdf =
                                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
                            Date d1 = sdf.parse(n1.getCreatedAt());
                            Date d2 = sdf.parse(n2.getCreatedAt());
                            if (d1 == null || d2 == null) return 0;
                            return Long.compare(d2.getTime(), d1.getTime());
                        } catch (ParseException e) {
                            return n2.getCreatedAt().compareTo(n1.getCreatedAt());
                        }
                    });

                    int size = Math.min(4, allNotices.size());
                    for (int i = 0; i < size; i++) {
                        Notice n = allNotices.get(i);
                        recentNotices.add(n);

                        String relativeTime = getRelativeTime(n.getCreatedAt());
                        String academyLabel = (n.getAcademyName() != null && !n.getAcademyName().isEmpty())
                                ? n.getAcademyName()
                                : ("학원 " + n.getAcademyNumber());

                        String text = "· [" + academyLabel + "] " + n.getTitle() + " (" + relativeTime + ")";

                        TextView targetView = (i == 0) ? tvNotice1
                                : (i == 1) ? tvNotice2
                                : (i == 2) ? tvNotice3
                                : tvNotice4;
                        if (targetView != null) {
                            targetView.setText(text);
                        }
                    }

                    blockNotice1.setOnClickListener(v -> openNoticeDetailAtIndex(0));
                    blockNotice2.setOnClickListener(v -> openNoticeDetailAtIndex(1));
                    blockNotice3.setOnClickListener(v -> openNoticeDetailAtIndex(2));
                    blockNotice4.setOnClickListener(v -> openNoticeDetailAtIndex(3));

                } else {
                    tvNotice1.setText("공지 없음");
                    tvNotice2.setText("");
                    tvNotice3.setText("");
                    if (tvNotice4 != null) tvNotice4.setText("");
                }
            }

            @Override
            public void onFailure(Call<List<Notice>> call, Throwable t) {
                tvNotice1.setText("공지 불러오기 실패");
                tvNotice2.setText("");
                tvNotice3.setText("");
                if (tvNotice4 != null) tvNotice4.setText("");
            }
        });
    }

    private void openNoticeDetailAtIndex(int index) {
        if (index < recentNotices.size()) {
            Notice notice = recentNotices.get(index);
            openNoticeDetail(notice);
        }
    }

    private void openNoticeDetail(Notice notice) {
        Intent intent = new Intent(MainActivity.this, NoticeDetailActivity.class);
        intent.putExtra("NOTICE_ID", String.valueOf(notice.getId()));
        startActivity(intent);
    }

    /** 부모 계정 자녀 선택 */
    private void handleAttendanceClick() {
        String parentId = prefs.getString("userId", "");
        ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
        api.getChildrenByParentId(parentId).enqueue(new Callback<List<Student>>() {
            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    Log.e(TAG, "자녀 목록 없음/실패");
                    return;
                }
                List<Student> children = response.body();
                String[] names = new String[children.size()];
                for (int i = 0; i < children.size(); i++)
                    names[i] = children.get(i).getStudentName();

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("자녀 선택")
                        .setItems(names, (d, which) -> {
                            Student s = children.get(which);
                            tvSelectedChild.setText(s.getStudentName() + " 님");
                            prefs.edit()
                                    .putString("selected_child", s.getStudentName())
                                    .putString("selected_child_id", s.getStudentId())
                                    .apply();
                            refreshCurrentAttendance();
                        })
                        .show();
            }

            @Override
            public void onFailure(Call<List<Student>> call, Throwable t) {
                Log.e(TAG, "자녀 목록 실패: " + t.getMessage());
            }
        });
    }

    /** 출결 */
    private void refreshCurrentAttendance() {
        String studentId = resolveCurrentStudentId();
        if (studentId == null || studentId.trim().isEmpty()) return;

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
        api.getAttendanceForStudent(studentId).enqueue(new Callback<List<AttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "출결 응답 실패: " + response.code());
                    return;
                }
                List<AttendanceResponse> list = response.body();
                if (list == null) list = new ArrayList<>();

                int attend = 0, late = 0, absent = 0;
                for (AttendanceResponse ar : list) {
                    String s = normalizeStatus(ar.getStatus());
                    if ("출석".equals(s)) attend++;
                    else if ("지각".equals(s)) late++;
                    else if ("결석".equals(s)) absent++;
                }

                tvAttend.setText("출석: " + attend);
                tvLate.setText("지각: " + late);
                tvAbsent.setText("결석: " + absent);
            }

            @Override
            public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                Log.e(TAG, "출결 로드 실패: " + t.getMessage());
            }
        });
    }

    private String resolveCurrentStudentId() {
        String role = prefs.getString("role", "");
        if ("parent".equalsIgnoreCase(role)) {
            return prefs.getString("selected_child_id", "");
        }
        return prefs.getString("userId", "");
    }

    /** 🔹 최근 QnA (최신 2개) */
    private void loadRecentQna() {
        String token = prefs.getString("token", "");
        String authHeader = "Bearer " + token;

        AnswerApi api = RetrofitClient.getClient().create(AnswerApi.class);
        api.listAnswers(authHeader, "2").enqueue(new Callback<List<Answer>>() {
            @Override
            public void onResponse(Call<List<Answer>> call, Response<List<Answer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recentAnswers.clear();
                    recentAnswers.addAll(response.body());

                    if (recentAnswers.size() > 0) {
                        Answer a1 = recentAnswers.get(0);
                        String display1 = (a1.getTeacherName() != null ? a1.getTeacherName() + " 선생님: " : "") + a1.getContent();
                        tvQna1.setText(display1);
                    } else {
                        tvQna1.setText("· 최근 답변 없음");
                    }

                    if (recentAnswers.size() > 1) {
                        Answer a2 = recentAnswers.get(1);
                        String display2 = (a2.getTeacherName() != null ? a2.getTeacherName() + " 선생님: " : "") + a2.getContent();
                        tvQna2.setText(display2);
                    } else {
                        tvQna2.setText("· 최근 답변 없음");
                    }
                } else {
                    tvQna1.setText("· 최근 답변 없음");
                    tvQna2.setText("");
                }
            }

            @Override
            public void onFailure(Call<List<Answer>> call, Throwable t) {
                Log.e(TAG, "QnA 실패: " + t.getMessage());
                tvQna1.setText("QnA 불러오기 실패");
                tvQna2.setText("");
            }
        });
    }

    private void openQnaDetailAtIndex(int index) {
        if (index < recentAnswers.size()) {
            Answer ans = recentAnswers.get(index);
            Intent intent = new Intent(MainActivity.this, QuestionDetailActivity.class);
            intent.putExtra("QUESTION_ID", String.valueOf(ans.getQuestionId()));
            startActivity(intent);
        }
    }

    /** helpers */
    private static String normalizeStatus(String s) {
        if (s == null) return "";
        String raw = s.trim();
        String compact = raw.replaceAll("\\s+", "");
        if (compact.contains("결석")) return "결석";
        if (compact.contains("지각")) return "지각";
        if (compact.contains("출석")) return "출석";
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("absent") || lower.contains("absence")) return "결석";
        if (lower.startsWith("late") || lower.contains("tardy")) return "지각";
        if (lower.startsWith("present") || lower.contains("attend")) return "출석";
        return raw;
    }

    private static String getRelativeTime(String isoTime) {
        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(isoTime);
            if (date == null) return "";
            long now = System.currentTimeMillis();
            long diffMillis = now - date.getTime();
            long diffDays = diffMillis / (1000 * 60 * 60 * 24);

            if (diffDays == 0) return "오늘";
            else if (diffDays == 1) return "어제";
            else return diffDays + "일 전";

        } catch (Exception e) {
            e.printStackTrace();
            return isoTime;
        }
    }
}
