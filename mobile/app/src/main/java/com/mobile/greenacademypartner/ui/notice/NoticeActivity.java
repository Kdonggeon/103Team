package com.mobile.greenacademypartner.ui.notice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.NoticeApi;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.Notice;
import com.mobile.greenacademypartner.model.student.Student;
import com.mobile.greenacademypartner.ui.attendance.AttendanceActivity;
import com.mobile.greenacademypartner.ui.main.MainActivity;
import com.mobile.greenacademypartner.ui.mypage.MyPageActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;
import com.mobile.greenacademypartner.ui.timetable.QRScannerActivity;
import com.mobile.greenacademypartner.ui.timetable.StudentTimetableActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NoticeActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView rvNotices;
    private ProgressBar progressBar;
    private Button btnAdd;
    private Spinner spinnerAcademy;

    private NoticeApi noticeApi;
    private ParentApi parentApi;
    private StudentApi studentApi;

    private final List<Integer> userAcademyNumbers = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    // 하단 네비 + 토글
    private ImageButton btnHideNav, btnShowNav;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);

        // 1) 뷰 바인딩
        toolbar = findViewById(R.id.toolbar_notice);
        rvNotices = findViewById(R.id.rv_notices);
        progressBar = findViewById(R.id.pb_loading_notices);
        btnAdd = findViewById(R.id.btn_add_notice);
        spinnerAcademy = findViewById(R.id.spinner_academy);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnHideNav = findViewById(R.id.btn_hide_nav);
        btnShowNav = findViewById(R.id.btn_show_nav);

        // 2) 툴바/테마
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);
        ThemeColorUtil.applyThemeColor(this, toolbar);

        // 3) RecyclerView & API
        rvNotices.setLayoutManager(new LinearLayoutManager(this));
        noticeApi = RetrofitClient.getClient().create(NoticeApi.class);
        parentApi = RetrofitClient.getClient().create(ParentApi.class);
        studentApi = RetrofitClient.getClient().create(StudentApi.class);

        // 4) 권한에 따른 버튼 표시 + 스피너 데이터 로드
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", "");
        if (!"teacher".equalsIgnoreCase(role) && !"director".equalsIgnoreCase(role)) {
            btnAdd.setVisibility(View.GONE); // 학생/학부모는 공지 등록 버튼 숨김
        }
        loadAcademyNumbersForRole(role, prefs);

        // 5) 공지 등록 버튼
        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, CreateNoticeActivity.class))
        );

        // 6) 하단 네비게이션
        bottomNavigationView.setSelectedItemId(R.id.nav_notice);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_attendance) {
                startActivity(new Intent(this, AttendanceActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_qr) {
                startActivity(new Intent(this, QRScannerActivity.class));
                return true;
            } else if (id == R.id.nav_timetable) {
                startActivity(new Intent(this, StudentTimetableActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_my) {
                startActivity(new Intent(this, MyPageActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // 7) 네비 토글
        btnHideNav.setOnClickListener(v -> {
            bottomNavigationView.setVisibility(View.GONE);
            btnHideNav.setVisibility(View.GONE);
            btnShowNav.setVisibility(View.VISIBLE);
        });
        btnShowNav.setOnClickListener(v -> {
            bottomNavigationView.setVisibility(View.VISIBLE);
            btnShowNav.setVisibility(View.GONE);
            btnHideNav.setVisibility(View.VISIBLE);
        });
    }

    /**
     * 역할별로 스피너에 표시할 학원 번호를 채운다.
     * - teacher/director : prefs의 academyNumbers 사용
     * - student : prefs의 academyNumbers 사용, 비어있으면 StudentApi로 학생 조회 후 보강
     * - parent  : 자녀 목록을 불러와 자녀의 academyNumbers 사용 (중복 제거)
     */
    private void loadAcademyNumbersForRole(String role, SharedPreferences prefs) {
        userAcademyNumbers.clear();

        // 🧩 교직원
        if ("teacher".equalsIgnoreCase(role) || "director".equalsIgnoreCase(role)) {
            String academyArray = prefs.getString("academyNumbers", "[]");
            try {
                JSONArray arr = new JSONArray(academyArray);
                for (int i = 0; i < arr.length(); i++) {
                    userAcademyNumbers.add(arr.getInt(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            setupSpinnerAndFetch();
            return;
        }

        // 🧩 학생
        if ("student".equalsIgnoreCase(role)) {
            String academyArray = prefs.getString("academyNumbers", "[]");
            try {
                JSONArray arr = new JSONArray(academyArray);
                for (int i = 0; i < arr.length(); i++) {
                    userAcademyNumbers.add(arr.getInt(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (!userAcademyNumbers.isEmpty()) {
                setupSpinnerAndFetch();
                return;
            }

            // 폴백: 서버 조회
            String studentId = prefs.getString("username", "");
            if (studentId == null || studentId.trim().isEmpty()) {
                Toast.makeText(this, "학생 계정 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                setupSpinnerAndFetch();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            studentApi.getStudentById(studentId).enqueue(new Callback<Student>() {
                @Override
                public void onResponse(Call<Student> call, Response<Student> response) {
                    progressBar.setVisibility(View.GONE);
                    if (!response.isSuccessful() || response.body() == null) {
                        Toast.makeText(NoticeActivity.this,
                                "학생 정보 조회 실패: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                        setupSpinnerAndFetch();
                        return;
                    }
                    List<Integer> academies = response.body().getAcademyNumbers();
                    if (academies != null) {
                        // 중복 제거
                        userAcademyNumbers.addAll(new LinkedHashSet<>(academies));
                        // 로컬 prefs에도 저장(다음 진입 시 빠르게 표시)
                        prefs.edit().putString("academyNumbers",
                                new JSONArray(academies).toString()).apply();
                    }
                    setupSpinnerAndFetch();
                }

                @Override
                public void onFailure(Call<Student> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(NoticeActivity.this,
                            "학생 정보 네트워크 오류: " + t.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    setupSpinnerAndFetch();
                }
            });
            return;
        }

        // 🧩 학부모
        String parentId = prefs.getString("userId", "");
        if (parentId == null || parentId.trim().isEmpty()) {
            Toast.makeText(this, "학부모 계정 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            setupSpinnerAndFetch();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        parentApi.getChildrenByParentId(parentId).enqueue(new Callback<List<Student>>() {
            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {
                progressBar.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(NoticeActivity.this,
                            "자녀 조회 실패: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                    setupSpinnerAndFetch();
                    return;
                }

                // 🔥 Null-safe 학원번호 수집
                Set<Integer> academySet = new LinkedHashSet<>();

                for (Student s : response.body()) {
                    List<Integer> academies = s.getAcademyNumbers();
                    if (academies != null) {
                        for (Integer num : academies) {
                            if (num != null && num > 0) {
                                academySet.add(num);
                            }
                        }
                    }
                }

                userAcademyNumbers.clear();
                userAcademyNumbers.addAll(academySet);

                if (userAcademyNumbers.isEmpty()) {
                    Toast.makeText(NoticeActivity.this,
                            "등록된 자녀의 학원이 없습니다.",
                            Toast.LENGTH_SHORT).show();
                }

                setupSpinnerAndFetch();
            }

            @Override
            public void onFailure(Call<List<Student>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NoticeActivity.this,
                        "자녀 조회 네트워크 오류: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                setupSpinnerAndFetch();
            }
        });
    }

    /** 스피너 구성 및 최초 fetch */
    private void setupSpinnerAndFetch() {
        List<String> labels = new ArrayList<>();
        for (Integer num : userAcademyNumbers) {
            labels.add("학원 " + num);
        }

        spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAcademy.setAdapter(spinnerAdapter);

        spinnerAcademy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < userAcademyNumbers.size()) {
                    fetchNotices(userAcademyNumbers.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 최초 로드
        if (!userAcademyNumbers.isEmpty()) {
            fetchNotices(userAcademyNumbers.get(0));
        } else {
            rvNotices.setAdapter(new NoticeListAdapter(new ArrayList<>(), n -> {}));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 테마 재적용
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        ThemeColorUtil.applyThemeColor(this, toolbar);

        // 현재 선택된 학원 재조회
        if (!userAcademyNumbers.isEmpty() && spinnerAcademy.getSelectedItemPosition() >= 0) {
            int idx = spinnerAcademy.getSelectedItemPosition();
            if (idx >= 0 && idx < userAcademyNumbers.size()) {
                fetchNotices(userAcademyNumbers.get(idx));
            }
        }
    }

    private void fetchNotices(int academyNumber) {
        progressBar.setVisibility(View.VISIBLE);
        noticeApi.getNoticesByAcademy(academyNumber).enqueue(new Callback<List<Notice>>() {
            @Override
            public void onResponse(Call<List<Notice>> call, Response<List<Notice>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    NoticeListAdapter adapter = new NoticeListAdapter(
                            response.body(),
                            notice -> {
                                Intent it = new Intent(NoticeActivity.this, NoticeDetailActivity.class);
                                it.putExtra("notice_id", notice.getId());
                                startActivity(it);
                            }
                    );
                    rvNotices.setAdapter(adapter);
                } else {
                    Toast.makeText(NoticeActivity.this,
                            "목록 조회 실패: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Notice>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NoticeActivity.this,
                        "네트워크 오류: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
