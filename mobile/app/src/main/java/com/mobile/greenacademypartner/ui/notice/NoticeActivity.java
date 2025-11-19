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
import com.mobile.greenacademypartner.api.AcademyApi;
import com.mobile.greenacademypartner.api.NoticeApi;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.model.Academy;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private AcademyApi academyApi;

    private final List<Integer> userAcademyNumbers = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    private ImageButton btnHideNav, btnShowNav;
    private BottomNavigationView bottomNavigationView;

    // 🔥 학원이름 → 학원번호 매핑
    private final Map<String, Integer> academyMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);

        toolbar = findViewById(R.id.toolbar_notice);
        rvNotices = findViewById(R.id.rv_notices);
        progressBar = findViewById(R.id.pb_loading_notices);
        btnAdd = findViewById(R.id.btn_add_notice);
        spinnerAcademy = findViewById(R.id.spinner_academy);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnHideNav = findViewById(R.id.btn_hide_nav);
        btnShowNav = findViewById(R.id.btn_show_nav);

        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);
        ThemeColorUtil.applyThemeColor(this, toolbar);

        rvNotices.setLayoutManager(new LinearLayoutManager(this));

        noticeApi = RetrofitClient.getClient().create(NoticeApi.class);
        parentApi = RetrofitClient.getClient().create(ParentApi.class);
        studentApi = RetrofitClient.getClient().create(StudentApi.class);
        academyApi = RetrofitClient.getClient().create(AcademyApi.class);

        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", "");

        if (!"teacher".equalsIgnoreCase(role) && !"director".equalsIgnoreCase(role)) {
            btnAdd.setVisibility(View.GONE);
        }

        loadAcademyNumbersForRole(role, prefs);

        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, CreateNoticeActivity.class))
        );

        bottomNavigationView.setSelectedItemId(R.id.nav_notice);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.nav_attendance) startActivity(new Intent(this, AttendanceActivity.class));
            else if (id == R.id.nav_qr) startActivity(new Intent(this, QRScannerActivity.class));
            else if (id == R.id.nav_timetable) startActivity(new Intent(this, StudentTimetableActivity.class));
            else if (id == R.id.nav_my) startActivity(new Intent(this, MyPageActivity.class));
            return true;
        });

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

    /** 학원 번호 로딩 */
    private void loadAcademyNumbersForRole(String role, SharedPreferences prefs) {
        userAcademyNumbers.clear();

        if ("teacher".equalsIgnoreCase(role) || "director".equalsIgnoreCase(role)) {
            String arr = prefs.getString("academyNumbers", "[]");
            try {
                JSONArray json = new JSONArray(arr);
                for (int i = 0; i < json.length(); i++)
                    userAcademyNumbers.add(json.getInt(i));
            } catch (JSONException ignored) {}
            loadAcademyNames();
            return;
        }

        if ("student".equalsIgnoreCase(role)) {
            String arr = prefs.getString("academyNumbers", "[]");

            try {
                JSONArray json = new JSONArray(arr);
                for (int i = 0; i < json.length(); i++)
                    userAcademyNumbers.add(json.getInt(i));
            } catch (JSONException ignored) {}

            if (!userAcademyNumbers.isEmpty()) {
                loadAcademyNames();
                return;
            }

            // 폴백
            String studentId = prefs.getString("username", "");
            if (studentId.isEmpty()) {
                loadAcademyNames();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            studentApi.getStudentById(studentId).enqueue(new Callback<Student>() {
                @Override
                public void onResponse(Call<Student> call, Response<Student> response) {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        userAcademyNumbers.addAll(
                                new LinkedHashSet<>(response.body().getAcademyNumbers())
                        );
                    }
                    loadAcademyNames();
                }

                @Override
                public void onFailure(Call<Student> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    loadAcademyNames();
                }
            });
            return;
        }

        // 학부모
        String parentId = prefs.getString("userId", "");
        if (parentId.isEmpty()) {
            loadAcademyNames();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        parentApi.getChildrenByParentId(parentId).enqueue(new Callback<List<Student>>() {
            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    Set<Integer> set = new LinkedHashSet<>();
                    for (Student s : response.body()) {
                        if (s.getAcademyNumbers() != null)
                            set.addAll(s.getAcademyNumbers());
                    }
                    userAcademyNumbers.addAll(set);
                }
                loadAcademyNames();
            }

            @Override
            public void onFailure(Call<List<Student>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                loadAcademyNames();
            }
        });
    }

    /** 학원이름 로딩 + 매핑 */
    private void loadAcademyNames() {

        if (userAcademyNumbers.isEmpty()) {
            spinnerAcademy.setAdapter(null);
            rvNotices.setAdapter(new NoticeListAdapter(new ArrayList<>(), n -> {}));
            return;
        }

        academyApi.getAcademyList().enqueue(new Callback<List<Academy>>() {
            @Override
            public void onResponse(Call<List<Academy>> call, Response<List<Academy>> response) {

                List<String> academyNames = new ArrayList<>();
                academyMap.clear();

                if (!response.isSuccessful() || response.body() == null) {
                    for (Integer num : userAcademyNumbers) {
                        String name = "학원 " + num;
                        academyNames.add(name);
                        academyMap.put(name, num);
                    }
                    setupSpinner(academyNames);
                    return;
                }

                List<Academy> all = response.body();

                for (Integer num : userAcademyNumbers) {
                    Academy match = null;

                    for (Academy a : all) {
                        if (a.getAcademyNumber() == num) {
                            match = a;
                            break;
                        }
                    }

                    if (match != null) {
                        academyNames.add(match.getAcademyName());
                        academyMap.put(match.getAcademyName(), match.getAcademyNumber());
                    } else {
                        String fallback = "학원 " + num;
                        academyNames.add(fallback);
                        academyMap.put(fallback, num);
                    }
                }

                setupSpinner(academyNames);
            }

            @Override
            public void onFailure(Call<List<Academy>> call, Throwable t) {
                List<String> fallback = new ArrayList<>();
                academyMap.clear();

                for (Integer num : userAcademyNumbers) {
                    String name = "학원 " + num;
                    fallback.add(name);
                    academyMap.put(name, num);
                }

                setupSpinner(fallback);
            }
        });
    }

    /** 스피너 구성 */
    private void setupSpinner(List<String> academyNames) {

        spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                academyNames
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAcademy.setAdapter(spinnerAdapter);

        spinnerAcademy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selectedName = spinnerAdapter.getItem(position);
                int academyNumber = academyMap.get(selectedName);

                fetchNotices(academyNumber);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // default load
        if (!academyNames.isEmpty()) {
            String firstName = academyNames.get(0);
            int academyNumber = academyMap.get(firstName);
            fetchNotices(academyNumber);
        }
    }

    /** 공지 조회 - academyNumbers 배열까지 포함한 완성본 */
    private void fetchNotices(int academyNumber) {
        progressBar.setVisibility(View.VISIBLE);

        // 1) 단일 academyNumber 공지
        noticeApi.getNoticesByAcademy(academyNumber).enqueue(new Callback<List<Notice>>() {
            @Override
            public void onResponse(Call<List<Notice>> call1, Response<List<Notice>> res1) {

                List<Notice> result = new ArrayList<>();
                if (res1.isSuccessful() && res1.body() != null) {
                    result.addAll(res1.body());
                }

                // 2) 전체 공지 → academyNumbers 배열 필터링
                noticeApi.listNotices().enqueue(new Callback<List<Notice>>() {
                    @Override
                    public void onResponse(Call<List<Notice>> call2, Response<List<Notice>> res2) {
                        progressBar.setVisibility(View.GONE);

                        if (res2.isSuccessful() && res2.body() != null) {
                            for (Notice n : res2.body()) {

                                List<Integer> arr = n.getAcademyNumbers();
                                if (arr != null && arr.contains(academyNumber)) {
                                    result.add(n);
                                }
                            }
                        }

                        // 🔥 최종 결과 표시
                        rvNotices.setAdapter(new NoticeListAdapter(
                                result,
                                notice -> {
                                    Intent it = new Intent(NoticeActivity.this, NoticeDetailActivity.class);
                                    it.putExtra("notice_id", notice.getId());
                                    startActivity(it);
                                }
                        ));
                    }

                    @Override
                    public void onFailure(Call<List<Notice>> call2, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onFailure(Call<List<Notice>> call1, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}
