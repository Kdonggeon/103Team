package com.mobile.greenacademypartner.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import org.json.JSONArray;
import org.json.JSONException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date; // 남겨둠: 레거시 포맷 대응용
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    private TextView tvQna1, tvQna2;
    private LinearLayout blockQna1, blockQna2;
    private final List<Answer> recentAnswers = new ArrayList<>();

    private TextView tvNotice1, tvNotice2, tvNotice3, tvNotice4;
    private LinearLayout blockNotice1, blockNotice2, blockNotice3, blockNotice4;
    private final List<Notice> recentNotices = new ArrayList<>();

    private TextView tvAttend, tvLate, tvAbsent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        tvSelectedChild = findViewById(R.id.tvSelectedChild);
        btnAttendance = findViewById(R.id.btnAttendance);

        tvQna1 = findViewById(R.id.tvQna1);
        tvQna2 = findViewById(R.id.tvQna2);
        blockQna1 = findViewById(R.id.blockQna1);
        blockQna2 = findViewById(R.id.blockQna2);

        tvNotice1 = findViewById(R.id.tvNotice1);
        tvNotice2 = findViewById(R.id.tvNotice2);
        tvNotice3 = findViewById(R.id.tvNotice3);
        tvNotice4 = findViewById(R.id.tvNotice4);
        blockNotice1 = findViewById(R.id.blockNotice1);
        blockNotice2 = findViewById(R.id.blockNotice2);
        blockNotice3 = findViewById(R.id.blockNotice3);
        blockNotice4 = findViewById(R.id.blockNotice4);

        tvAttend = findViewById(R.id.tvAttend);
        tvLate = findViewById(R.id.tvLate);
        tvAbsent = findViewById(R.id.tvAbsent);

        ImageView btnMoreQna = findViewById(R.id.btnMoreQna);
        if (btnMoreQna != null) btnMoreQna.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, QuestionsActivity.class)));

        ImageView btnMoreNotice = findViewById(R.id.btnMoreNotice);
        if (btnMoreNotice != null) btnMoreNotice.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NoticeActivity.class)));

        blockQna1.setOnClickListener(v -> openQnaDetailAtIndex(0));
        blockQna2.setOnClickListener(v -> openQnaDetailAtIndex(1));

        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingActivity.class)));
        findViewById(R.id.btnNotifications).setOnClickListener(v -> startActivity(new Intent(this, com.mobile.greenacademypartner.ui.notification.NotificationActivity.class)));

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_attendance) { startActivity(new Intent(this, AttendanceActivity.class)); return true; }
            if (id == R.id.nav_qr) { startActivity(new Intent(this, QRScannerActivity.class)); return true; }
            if (id == R.id.nav_timetable) { startActivity(new Intent(this, StudentTimetableActivity.class)); return true; }
            if (id == R.id.nav_my) { startActivity(new Intent(this, MyPageActivity.class)); return true; }
            return false;
        });

        String role = prefs.getString("role", "");
        String studentName = prefs.getString("student_name", "");
        if ("student".equalsIgnoreCase(role)) {
            if (!studentName.isEmpty()) tvSelectedChild.setText(studentName + " 님");
            btnAttendance.setVisibility(android.view.View.GONE);
            loadRecentQna();
            loadRecentNoticesAll();
            refreshCurrentAttendance();
        } else if ("parent".equalsIgnoreCase(role)) {
            btnAttendance.setVisibility(android.view.View.VISIBLE);
            btnAttendance.setOnClickListener(v -> handleAttendanceClick());
            String selectedChild = prefs.getString("selected_child", "");
            String selectedChildId = prefs.getString("selected_child_id", "");
            if (selectedChildId == null || selectedChildId.isEmpty()) {
                tvSelectedChild.setText("자녀 선택 님");
                clearQna();
                clearNotices();
                clearAttendance();
            } else {
                tvSelectedChild.setText(selectedChild + " 님");
                loadRecentQna();
                loadRecentNoticesAll();
                refreshCurrentAttendance();
            }
        } else {
            tvSelectedChild.setText("");
            clearQna();
            clearNotices();
            clearAttendance();
        }
    }

    private void clearQna() {
        recentAnswers.clear();
        tvQna1.setText("· 최근 답변 없음");
        tvQna2.setText("· 최근 답변 없음");
    }

    private void clearNotices() {
        recentNotices.clear();
        tvNotice1.setText("· 최근 공지 없음");
        tvNotice2.setText("· 최근 공지 없음");
        tvNotice3.setText("· 최근 공지 없음");
        if (tvNotice4 != null) tvNotice4.setText("· 최근 공지 없음");
        blockNotice1.setOnClickListener(null);
        blockNotice2.setOnClickListener(null);
        blockNotice3.setOnClickListener(null);
        blockNotice4.setOnClickListener(null);
    }

    private void clearAttendance() {
        tvAttend.setText("출석: 0");
        tvLate.setText("지각: 0");
        tvAbsent.setText("결석: 0");
    }

    private void handleAttendanceClick() {
        String parentId = prefs.getString("userId", "");
        ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
        api.getChildrenByParentId(parentId).enqueue(new Callback<List<Student>>() {
            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> res) {
                if (!res.isSuccessful() || res.body() == null || res.body().isEmpty()) return;
                List<Student> children = res.body();
                String[] names = new String[children.size()];
                for (int i = 0; i < children.size(); i++) names[i] = children.get(i).getStudentName();
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("자녀 선택")
                        .setItems(names, (d, which) -> {
                            Student s = children.get(which);
                            tvSelectedChild.setText(s.getStudentName() + " 님");
                            prefs.edit()
                                    .putString("selected_child", s.getStudentName())
                                    .putString("selected_child_id", s.getStudentId())
                                    .apply();
                            if (s.getAcademy_Numbers() != null && !s.getAcademy_Numbers().isEmpty()) {
                                loadRecentQna();
                                loadRecentNoticesAll();
                            } else {
                                clearQna();
                                clearNotices();
                            }
                            refreshCurrentAttendance();
                        })
                        .show();
            }
            @Override public void onFailure(Call<List<Student>> call, Throwable t) { Log.e(TAG, "자녀 목록 실패: " + t.getMessage()); }
        });
    }

    /** 백엔드 그대로 두고 전체 공지를 받아온 뒤, 클라이언트에서 학원별 필터 */
    private void loadRecentNoticesAll() {
        if (isParentAndChildNotSelected()) { clearNotices(); return; }
        NoticeApi api = RetrofitClient.getClient().create(NoticeApi.class);
        api.listNotices().enqueue(new Callback<List<Notice>>() {
            @Override
            public void onResponse(Call<List<Notice>> call, Response<List<Notice>> res) {
                if (!res.isSuccessful() || res.body() == null) { clearNotices(); return; }
                List<Notice> all = res.body();
                String role = prefs.getString("role", "");
                if ("student".equalsIgnoreCase(role)) {
                    List<Integer> myAcademies = getStudentAcademyNumbers();
                    if (myAcademies.isEmpty()) {
                        String sid = prefs.getString("userId", "");
                        StudentApi sApi = RetrofitClient.getClient().create(StudentApi.class);
                        sApi.getStudentById(sid).enqueue(new Callback<Student>() {
                            @Override
                            public void onResponse(Call<Student> call, Response<Student> resp) {
                                List<Integer> nums = new ArrayList<>();
                                if (resp.isSuccessful() && resp.body() != null && resp.body().getAcademy_Numbers() != null) {
                                    nums.addAll(resp.body().getAcademy_Numbers());
                                }
                                showFilteredNotices(all, nums);
                            }
                            @Override public void onFailure(Call<Student> call, Throwable t) { showFilteredNotices(all, new ArrayList<>()); }
                        });
                    } else {
                        showFilteredNotices(all, myAcademies);
                    }
                } else if ("parent".equalsIgnoreCase(role)) {
                    String pid = prefs.getString("userId", "");
                    ParentApi parentApi = RetrofitClient.getClient().create(ParentApi.class);
                    parentApi.getChildrenByParentId(pid).enqueue(new Callback<List<Student>>() {
                        @Override
                        public void onResponse(Call<List<Student>> call, Response<List<Student>> res2) {
                            List<Integer> nums = new ArrayList<>();
                            if (res2.isSuccessful() && res2.body() != null) {
                                for (Student s : res2.body()) {
                                    if (s.getAcademy_Numbers() != null && !s.getAcademy_Numbers().isEmpty()) nums.addAll(s.getAcademy_Numbers());
                                }
                            }
                            showFilteredNotices(all, nums);
                        }
                        @Override public void onFailure(Call<List<Student>> call, Throwable t) { clearNotices(); }
                    });
                }
            }
            @Override public void onFailure(Call<List<Notice>> call, Throwable t) { clearNotices(); }
        });
    }

    /**
     * 핵심 수정:
     * - 필터 결과가 비어도 all을 다시 넣지 않음 (다른 학원 공지 노출 금지)
     * - createdAt 최신순 정렬
     * - 상위 4개만 바인딩
     */
    private void showFilteredNotices(List<Notice> all, List<Integer> allowedNos) {
        List<Notice> filtered = new ArrayList<>();
        Set<Integer> allowed = new HashSet<>(allowedNos == null ? new ArrayList<>() : allowedNos);

        for (Notice n : all) {
            int aNo = n.getAcademyNumber();         // 전체공지라면 0 또는 음수로 온다고 가정
            boolean isGlobal = aNo <= 0;
            boolean permitted = isGlobal || allowed.contains(aNo);
            if (permitted) filtered.add(n);
        }

        // 필터 결과가 비면 빈 상태로 처리 (다른 학원 글 주입하지 않음)
        if (filtered.isEmpty()) {
            clearNotices();
            return;
        }

        // 최신순 정렬
        Collections.sort(filtered, new Comparator<Notice>() {
            @Override public int compare(Notice n1, Notice n2) {
                long t1 = parseCreatedAtToEpoch(n1.getCreatedAt());
                long t2 = parseCreatedAtToEpoch(n2.getCreatedAt());
                return Long.compare(t2, t1);
            }
        });

        recentNotices.clear();
        int size = Math.min(4, filtered.size());
        for (int i = 0; i < size; i++) {
            Notice n = filtered.get(i);
            recentNotices.add(n);
            String rel = getRelativeTime(n.getCreatedAt());
            String academyLabel = (n.getAcademyName() != null && !n.getAcademyName().isEmpty())
                    ? n.getAcademyName() : "학원 " + n.getAcademyNumber();
            String text = "· " + cleanTitle("[" + academyLabel + "] " + n.getTitle()) + " (" + rel + ")";
            TextView target = (i == 0) ? tvNotice1 : (i == 1) ? tvNotice2 : (i == 2) ? tvNotice3 : tvNotice4;
            if (target != null) target.setText(text);
        }

        // 클릭 리스너는 존재하는 인덱스만 연결
        blockNotice1.setOnClickListener(recentNotices.size() > 0 ? v -> openNoticeDetailAtIndex(0) : null);
        blockNotice2.setOnClickListener(recentNotices.size() > 1 ? v -> openNoticeDetailAtIndex(1) : null);
        blockNotice3.setOnClickListener(recentNotices.size() > 2 ? v -> openNoticeDetailAtIndex(2) : null);
        blockNotice4.setOnClickListener(recentNotices.size() > 3 ? v -> openNoticeDetailAtIndex(3) : null);
    }

    private long parseCreatedAtToEpoch(String createdAt) {
        if (createdAt == null) return 0L;
        // 우선 ISO-8601(Instant) 시도
        try {
            return Instant.parse(createdAt).toEpochMilli();
        } catch (DateTimeParseException ignored) {}
        // 레거시 포맷 시도: 2025-01-02T12:34:56.789+09:00 등
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
            return sdf.parse(createdAt).getTime();
        } catch (Exception ignored) {}
        return 0L;
    }

    private void openNoticeDetailAtIndex(int index) {
        if (index < recentNotices.size()) {
            Notice notice = recentNotices.get(index);
            Intent intent = new Intent(this, NoticeDetailActivity.class);
            intent.putExtra("NOTICE_ID", String.valueOf(notice.getId()));
            startActivity(intent);
        }
    }

    private void loadRecentQna() {
        if (isParentAndChildNotSelected()) { clearQna(); return; }
        String token = prefs.getString("token", "");
        AnswerApi api = RetrofitClient.getClient().create(AnswerApi.class);
        api.listAnswers("Bearer " + token, "2").enqueue(new Callback<List<Answer>>() {
            @Override public void onResponse(Call<List<Answer>> call, Response<List<Answer>> res) {
                if (!res.isSuccessful() || res.body() == null) { clearQna(); return; }
                recentAnswers.clear(); recentAnswers.addAll(res.body());
                if (!recentAnswers.isEmpty()) {
                    tvQna1.setText((recentAnswers.get(0).getTeacherName() != null ? recentAnswers.get(0).getTeacherName() + " 선생님: " : "") + recentAnswers.get(0).getContent());
                } else { tvQna1.setText("· 최근 답변 없음"); }
                if (recentAnswers.size() > 1) {
                    tvQna2.setText((recentAnswers.get(1).getTeacherName() != null ? recentAnswers.get(1).getTeacherName() + " 선생님: " : "") + recentAnswers.get(1).getContent());
                } else { tvQna2.setText("· 최근 답변 없음"); }
            }
            @Override public void onFailure(Call<List<Answer>> call, Throwable t) { clearQna(); }
        });
    }

    private void openQnaDetailAtIndex(int index) {
        if (index < recentAnswers.size()) {
            Answer answer = recentAnswers.get(index);
            Intent intent = new Intent(this, QuestionDetailActivity.class);
            intent.putExtra("QUESTION_ID", String.valueOf(answer.getQuestionId()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "해당 QnA 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshCurrentAttendance() {
        String studentId = ("parent".equalsIgnoreCase(prefs.getString("role", ""))) ? prefs.getString("selected_child_id", "") : prefs.getString("userId", "");
        if (studentId == null || studentId.isEmpty()) { clearAttendance(); return; }
        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
        api.getAttendanceForStudent(studentId).enqueue(new Callback<List<AttendanceResponse>>() {
            @Override public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> res) {
                if (!res.isSuccessful() || res.body() == null) { clearAttendance(); return; }
                int attend = 0, late = 0, absent = 0;
                for (AttendanceResponse a : res.body()) {
                    String s = normalizeStatus(a.getStatus());
                    if ("출석".equals(s)) attend++; else if ("지각".equals(s)) late++; else if ("결석".equals(s)) absent++;
                }
                tvAttend.setText("출석: " + attend);
                tvLate.setText("지각: " + late);
                tvAbsent.setText("결석: " + absent);
            }
            @Override public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) { clearAttendance(); }
        });
    }

    private boolean isParentAndChildNotSelected() {
        String role = prefs.getString("role", "");
        String childId = prefs.getString("selected_child_id", "");
        return "parent".equalsIgnoreCase(role) && (childId == null || childId.isEmpty());
    }

    private static String normalizeStatus(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.contains("absent")) return "결석";
        if (lower.contains("late")) return "지각";
        if (lower.contains("attend")) return "출석";
        return s;
    }

    private static String getRelativeTime(String iso) {
        try {
            long t;
            try {
                t = Instant.parse(iso).toEpochMilli();
            } catch (Exception e) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(iso);
                t = d.getTime();
            }
            long diffDay = (System.currentTimeMillis() - t) / (1000 * 60 * 60 * 24);
            return diffDay == 0 ? "오늘" : diffDay == 1 ? "어제" : diffDay + "일 전";
        } catch (Exception e) { return iso; }
    }

    private static String cleanTitle(String raw) {
        if (raw == null) return "";
        return raw.replaceFirst("^\\[[^\\]]+\\]\\s*", "");
    }

    private List<Integer> getStudentAcademyNumbers() {
        Set<Integer> set = new HashSet<>();
        int selected = prefs.getInt("selected_academy_number", -1);
        if (selected > 0) set.add(selected);
        int single = prefs.getInt("academyNumber", -1);
        if (single > 0) set.add(single);
        String json = prefs.getString("academy_numbers_json", "");
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    int n = arr.optInt(i, -1);
                    if (n > 0) set.add(n);
                }
            } catch (JSONException ignored) {}
        }
        String csv = prefs.getString("academy_numbers", "");
        if (csv != null && !csv.isEmpty()) {
            String[] parts = csv.split(",");
            for (String p : parts) {
                try {
                    int n = Integer.parseInt(p.trim());
                    if (n > 0) set.add(n);
                } catch (NumberFormatException ignored) {}
            }
        }
        return new ArrayList<>(set);
    }
}
