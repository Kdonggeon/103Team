package com.mobile.greenacademypartner.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.mobile.greenacademypartner.model.attendance.Attendance;
import com.mobile.greenacademypartner.model.attendance.AttendanceEntry;
import com.mobile.greenacademypartner.model.attendance.AttendanceResponse;
import com.mobile.greenacademypartner.model.classes.Course;
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

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
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
        initViews();

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

        setupNavigation();
    }


    private void initViews() {
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

        blockQna1.setOnClickListener(v -> openQnaDetailAtIndex(0));
        blockQna2.setOnClickListener(v -> openQnaDetailAtIndex(1));

        ImageView btnMoreQna = findViewById(R.id.btnMoreQna);
        if (btnMoreQna != null) {
            btnMoreQna.setOnClickListener(v ->
                    startActivity(new Intent(this, QuestionsActivity.class)));
        }

        ImageView btnMoreNotice = findViewById(R.id.btnMoreNotice);
        if (btnMoreNotice != null) {
            btnMoreNotice.setOnClickListener(v ->
                    startActivity(new Intent(this, NoticeActivity.class)));
        }

        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingActivity.class)));
    }


    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) return true;

            if (id == R.id.nav_attendance) {
                startActivity(new Intent(this, AttendanceActivity.class));
                return true;
            }
            if (id == R.id.nav_qr) {
                startActivity(new Intent(this, QRScannerActivity.class));
                return true;
            }
            if (id == R.id.nav_timetable) {
                startActivity(new Intent(this, StudentTimetableActivity.class));
                return true;
            }
            if (id == R.id.nav_my) {
                startActivity(new Intent(this, MyPageActivity.class));
                return true;
            }

            return false;
        });
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
                if (!res.isSuccessful() || res.body() == null || res.body().isEmpty()) {
                    tvSelectedChild.setText("자녀 선택 님");
                    clearQna();
                    clearNotices();
                    clearAttendance();
                    Toast.makeText(MainActivity.this, "등록된 자녀가 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Student> children = res.body();
                String[] names = new String[children.size()];
                for (int i = 0; i < children.size(); i++) {
                    names[i] = children.get(i).getStudentName();
                }

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("자녀 선택")
                        .setItems(names, (d, which) -> {
                            Student s = children.get(which);
                            tvSelectedChild.setText(s.getStudentName() + " 님");

                            prefs.edit()
                                    .putString("selected_child", s.getStudentName())
                                    .putString("selected_child_id", s.getStudentId())
                                    .apply();

                            loadRecentQna();
                            loadRecentNoticesAll();
                            refreshCurrentAttendance();
                        })
                        .show();
            }

            @Override
            public void onFailure(Call<List<Student>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "자녀 목록 불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadRecentNoticesAll() {
        String role = prefs.getString("role", "");

        if ("student".equalsIgnoreCase(role)) {
            List<Integer> myAcademies = getStudentAcademyNumbers();
            if (myAcademies == null || myAcademies.isEmpty()) {
                clearNotices();
                return;
            }
            fetchAndFilterNotices(myAcademies);
            return;
        }

        if ("parent".equalsIgnoreCase(role)) {
            String parentId = prefs.getString("userId", "");
            ParentApi api = RetrofitClient.getClient().create(ParentApi.class);

            api.getChildrenByParentId(parentId).enqueue(new Callback<List<Student>>() {
                @Override
                public void onResponse(Call<List<Student>> call, Response<List<Student>> res) {

                    if (!res.isSuccessful() || res.body() == null || res.body().isEmpty()) {
                        tvSelectedChild.setText("자녀 선택 님");
                        clearNotices();
                        clearQna();
                        clearAttendance();
                        return;
                    }

                    Set<Integer> academySet = new HashSet<>();

                    for (Student s : res.body()) {
                        List<Integer> nums = s.getAcademyNumbers();
                        if (nums != null) {
                            for (Integer n : nums) {
                                if (n != null && n > 0) academySet.add(n);
                            }
                        }
                    }

                    List<Integer> list = new ArrayList<>(academySet);

                    if (list.isEmpty()) {
                        clearNotices();
                        clearQna();
                        clearAttendance();
                        return;
                    }

                    fetchAndFilterNotices(list);
                }

                @Override
                public void onFailure(Call<List<Student>> call, Throwable t) {
                    clearNotices();
                }
            });
        }
    }


    private void fetchAndFilterNotices(List<Integer> allowedAcademyNos) {
        NoticeApi api = RetrofitClient.getClient().create(NoticeApi.class);

        api.listNotices().enqueue(new Callback<List<Notice>>() {
            @Override
            public void onResponse(Call<List<Notice>> call, Response<List<Notice>> res) {
                if (!res.isSuccessful() || res.body() == null) {
                    clearNotices();
                    return;
                }
                showFilteredNotices(res.body(), allowedAcademyNos);
            }

            @Override
            public void onFailure(Call<List<Notice>> call, Throwable t) {
                clearNotices();
            }
        });
    }


    private void showFilteredNotices(List<Notice> all, List<Integer> allowedNos) {

        Set<Integer> allowed = new HashSet<>(allowedNos == null ? new ArrayList<>() : allowedNos);
        List<Notice> filtered = new ArrayList<>();

        for (Notice n : all) {
            int aNo = n.getAcademyNumber();

            if (allowed.contains(aNo)) filtered.add(n);
        }

        if (filtered.isEmpty()) {
            clearNotices();
            return;
        }

        filtered.sort((n1, n2) ->
                Long.compare(parseCreatedAtToEpoch(n2.getCreatedAt()),
                        parseCreatedAtToEpoch(n1.getCreatedAt())));

        recentNotices.clear();

        int count = Math.min(4, filtered.size());
        for (int i = 0; i < count; i++) {
            Notice n = filtered.get(i);
            recentNotices.add(n);

            String rel = getRelativeTime(n.getCreatedAt());
            String academy =
                    (n.getAcademyName() != null && !n.getAcademyName().isEmpty())
                            ? n.getAcademyName()
                            : "학원 " + n.getAcademyNumber();

            String msg = "· [" + academy + "] " + n.getTitle() + " (" + rel + ")";

            TextView tv;
            switch (i) {
                case 0:
                    tv = tvNotice1;
                    break;
                case 1:
                    tv = tvNotice2;
                    break;
                case 2:
                    tv = tvNotice3;
                    break;
                default:
                    tv = tvNotice4;
                    break;
            }
            if (tv != null) {
                tv.setText(msg);
            }
        }

        blockNotice1.setOnClickListener(recentNotices.size() > 0 ? v -> openNoticeDetailAtIndex(0) : null);
        blockNotice2.setOnClickListener(recentNotices.size() > 1 ? v -> openNoticeDetailAtIndex(1) : null);
        blockNotice3.setOnClickListener(recentNotices.size() > 2 ? v -> openNoticeDetailAtIndex(2) : null);
        blockNotice4.setOnClickListener(recentNotices.size() > 3 ? v -> openNoticeDetailAtIndex(3) : null);
    }


    private void openNoticeDetailAtIndex(int index) {
        if (index < recentNotices.size()) {
            Notice n = recentNotices.get(index);
            Intent i = new Intent(this, NoticeDetailActivity.class);
            i.putExtra("NOTICE_ID", String.valueOf(n.getId()));
            startActivity(i);
        }
    }


    // ─────────────────────────────────────────────
    // QnA 최신 2개 가져오기 (NEW)
    // ─────────────────────────────────────────────
    private void loadRecentQna() {

        if (isParentAndChildNotSelected()) {
            clearQna();
            return;
        }

        String token = prefs.getString("token", "");
        if (token == null || token.isEmpty()) {
            clearQna();
            return;
        }

        AnswerApi api = RetrofitClient.getClient().create(AnswerApi.class);

        api.getMyRecentAnswers("Bearer " + token, 2)
                .enqueue(new Callback<List<Answer>>() {
            @Override
            public void onResponse(Call<List<Answer>> call, Response<List<Answer>> res) {

                if (!res.isSuccessful() || res.body() == null) {
                    clearQna();
                    return;
                }

                recentAnswers.clear();
                recentAnswers.addAll(res.body());

                if (recentAnswers.size() > 0)
                    tvQna1.setText("· " + recentAnswers.get(0).getContent());
                else
                    tvQna1.setText("· 최근 답변 없음");

                if (recentAnswers.size() > 1)
                    tvQna2.setText("· " + recentAnswers.get(1).getContent());
                else
                    tvQna2.setText("· 최근 답변 없음");
            }

            @Override
            public void onFailure(Call<List<Answer>> call, Throwable t) {
                clearQna();
            }
        });
    }


    private void openQnaDetailAtIndex(int index) {
        if (index >= recentAnswers.size()) {
            Toast.makeText(this, "해당 QnA 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Answer a = recentAnswers.get(index);

        // 🔥 questionId가 null 또는 비어있으면 바로 막기
        if (a.getQuestionId() == null || a.getQuestionId().trim().isEmpty() || "null".equals(a.getQuestionId())) {
            Toast.makeText(this, "해당 QnA는 원본 질문이 삭제되었거나 연결되지 않은 답변입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(this, QuestionDetailActivity.class);
        i.putExtra("QUESTION_ID", a.getQuestionId());
        startActivity(i);
    }


    private boolean isParentAndChildNotSelected() {
        String role = prefs.getString("role", "");
        String childId = prefs.getString("selected_child_id", "");
        return "parent".equalsIgnoreCase(role)
                && (childId == null || childId.isEmpty());
    }


    private void refreshCurrentAttendance() {

        String role = prefs.getString("role", "");

        String studentId = "parent".equalsIgnoreCase(role)
                ? prefs.getString("selected_child_id", "")
                : prefs.getString("userId", "");

        if (studentId == null || studentId.isEmpty()) {
            clearAttendance();
            return;
        }

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);

        // 1) 최근 7일 날짜 목록
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6); // 최근 7일

        api.getMyClasses(studentId).enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> classRes) {

                if (!classRes.isSuccessful() || classRes.body() == null) {
                    clearAttendance();
                    return;
                }

                List<Course> classList = classRes.body();

                // 2) 출석 데이터 전체 한번에 불러오기
                api.getAttendanceForStudent(studentId).enqueue(new Callback<List<AttendanceResponse>>() {
                    @Override
                    public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> attRes) {

                        if (!attRes.isSuccessful() || attRes.body() == null) {
                            clearAttendance();
                            return;
                        }

                        List<AttendanceResponse> attendanceList = attRes.body();

                        int present = 0, late = 0, absent = 0;

                        // 3) 최근 7일 날짜별로 계산
                        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {

                            String dateStr = date.toString();
                            int dow = date.getDayOfWeek().getValue();

                            // ■ 해당 날짜의 수업 목록 찾기
                            List<Course> dailyClasses = new ArrayList<>();
                            for (Course c : classList) {
                                if (c.getDaysOfWeek() != null && c.getDaysOfWeek().contains(dow)) {
                                    dailyClasses.add(c);
                                }
                            }

                            // ■ 해당 날짜 출석 기록 찾기
                            List<AttendanceResponse> dailyAttendance = new ArrayList<>();
                            for (AttendanceResponse ar : attendanceList) {
                                if (ar.getDate() != null && ar.getDate().startsWith(dateStr)) {
                                    dailyAttendance.add(ar);
                                }
                            }

                            // ■ 오늘 시간이 실제 필요함
                            LocalTime nowTime = LocalTime.now();

                            // 4) 출석 페이지 동일 로직 적용
                            for (Course c : dailyClasses) {

                                AttendanceResponse matched = null;

                                for (AttendanceResponse ar : dailyAttendance) {
                                    if (ar.getClassName().equals(c.getClassName())) {
                                        matched = ar;
                                        break;
                                    }
                                }

                                LocalTime classStart = LocalTime.parse(c.getStartTime());
                                LocalTime classEnd = LocalTime.parse(c.getEndTime());

                                // 미래 날짜 → 카운트 안함
                                if (date.isAfter(LocalDate.now())) continue;

                                // 과거 날짜 처리
                                if (date.isBefore(LocalDate.now())) {
                                    if (matched == null) {
                                        absent++;
                                    } else {
                                        String s = matched.getStatus();
                                        if (s.contains("출석")) present++;
                                        else if (s.contains("지각")) late++;
                                        else absent++;
                                    }
                                    continue;
                                }

                                // 오늘 날짜 처리
                                if (matched != null) {
                                    String s = matched.getStatus();
                                    if (s.contains("출석")) present++;
                                    else if (s.contains("지각")) late++;
                                    else absent++;
                                } else {
                                    if (nowTime.isAfter(classEnd)) {
                                        absent++;
                                    }
                                }
                            }
                        }

                        // 5) 메인 화면 표시
                        tvAttend.setText("출석: " + present);
                        tvLate.setText("지각: " + late);
                        tvAbsent.setText("결석: " + absent);
                    }

                    @Override
                    public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                        clearAttendance();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                clearAttendance();
            }
        });
    }



    private long parseDateFlexible(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return 0;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            return sdf.parse(dateStr).getTime();
        } catch (Exception ignore) {}

        try {
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
            return sdf2.parse(dateStr).getTime();
        } catch (Exception ignore) {}

        return 0;
    }



    private long parseDateSafe(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return 0;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            return sdf.parse(dateStr).getTime();
        } catch (Exception ignore) {}

        return 0;
    }


    private List<AttendanceResponse> filterLast30Days(List<AttendanceResponse> list) {
        List<AttendanceResponse> result = new ArrayList<>();

        long now = System.currentTimeMillis();
        long limit = now - (30L * 24L * 60L * 60L * 1000L); // 최근 30일 시간(ms)

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setLenient(false); // 잘못된 날짜 걸러냄

        for (AttendanceResponse a : list) {

            // 🔥 날짜 null/빈값 → 제외
            if (a.getDate() == null || a.getDate().trim().isEmpty())
                continue;

            try {
                Date d = sdf.parse(a.getDate()); // yyyy-MM-dd 로 파싱
                if (d != null && d.getTime() >= limit) {
                    result.add(a);
                }
            } catch (Exception ignore) {
                // 날짜 형식 잘못된 데이터도 제외
            }
        }

        return result;
    }





    private long parseCreatedAtToEpoch(String createdAt) {
        if (createdAt == null) return 0;
        try {
            return Instant.parse(createdAt).toEpochMilli();
        } catch (Exception ignore) {}

        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
            return sdf.parse(createdAt).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    private static String normalizeStatus(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.contains("attend")) return "출석";
        if (lower.contains("late")) return "지각";
        if (lower.contains("absent")) return "결석";
        return s;
    }

    private static String getRelativeTime(String iso) {
        try {
            long t;
            try {
                t = Instant.parse(iso).toEpochMilli();
            } catch (Exception e) {
                SimpleDateFormat sdf =
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(iso);
                t = d.getTime();
            }

            long diff = (System.currentTimeMillis() - t) / (1000 * 60 * 60 * 24);

            if (diff == 0) return "오늘";
            if (diff == 1) return "어제";
            return diff + "일 전";

        } catch (Exception e) {
            return iso;
        }
    }


    private List<Integer> getStudentAcademyNumbers() {
        Set<Integer> set = new HashSet<>();

        // 무조건 JSON 기준으로만 읽기
        String json = prefs.getString("academy_numbers_json", "");
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    int n = arr.optInt(i, -1);
                    if (n > 0) set.add(n);
                }
            } catch (Exception ignore) {}
        }

        return new ArrayList<>(set);
    }

}
