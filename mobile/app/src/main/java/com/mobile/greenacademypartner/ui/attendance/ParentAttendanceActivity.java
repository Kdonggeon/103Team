// ParentAttendanceActivity.java
package com.mobile.greenacademypartner.ui.attendance;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;

import com.mobile.greenacademypartner.menu.ToolbarIconUtil;
import com.mobile.greenacademypartner.model.attendance.Attendance;
import com.mobile.greenacademypartner.ui.adapter.ParentAttendanceAdapter;

import com.mobile.greenacademypartner.model.attendance.AttendanceResponse;
import com.mobile.greenacademypartner.model.classes.Course;
import com.mobile.greenacademypartner.model.student.Student;
import com.mobile.greenacademypartner.ui.adapter.AttendanceAdapter;

import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParentAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private RecyclerView attendanceListView;
    private Spinner spinnerChildren;

    // 요약 카드
    private TextView tvPresent, tvLate, tvAbsent;

    private SharedPreferences prefs;
    private StudentApi studentApi;
    private ParentApi parentApi;

    private AttendanceAdapter adapter;
    private final Map<String, List<Integer>> classDowMap = new HashMap<>(); // (정규화된) className → [daysOfWeek]
    private final List<AttendanceResponse> allAttendances = new ArrayList<>();

    // 비동기 레이스 가드
    private volatile String activeChildId = null;
    private Call<List<Course>> callClasses;
    private Call<List<AttendanceResponse>> callAttendance;

    // 현재 선택된 요일(1=월 … 7=일)
    private int currentDisplayDow = -1;

    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_PARENT_ID = "parentId";
    private static final String KEY_PARENT_ID_FALLBACK = "username";
    private static final String KEY_LAST_SELECTED = "last_selected_child_id";

    private static class ChildItem {
        final String id;
        final String name;
        ChildItem(String id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    private static final TimeZone KST = TimeZone.getTimeZone("Asia/Seoul");
    private static final Locale KOR = Locale.KOREA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_attendance);

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        attendanceListView = findViewById(R.id.attendance_list_view);
        spinnerChildren = findViewById(R.id.spinner_children);

        // 요약 카드 바인딩
        tvPresent = findViewById(R.id.tv_present_count);
        tvLate    = findViewById(R.id.tv_late_count);
        tvAbsent  = findViewById(R.id.tv_absent_count);

        int white = ContextCompat.getColor(this, android.R.color.white);
        toolbar.setTitleTextColor(white);
        if (toolbar.getNavigationIcon() != null) toolbar.getNavigationIcon().setTint(white);
        if (toolbar.getOverflowIcon() != null) toolbar.getOverflowIcon().setTint(white);

        setSupportActionBar(toolbar);
        setTitle("자녀 출석 확인");
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        ThemeColorUtil.applyThemeColor(this, toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        ToolbarIconUtil.applyWhiteIcons(toolbar, toggle);

        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, 1);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        studentApi = RetrofitClient.getClient().create(StudentApi.class);
        parentApi  = RetrofitClient.getClient().create(ParentApi.class);

        attendanceListView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(this, new ArrayList<>());
        attendanceListView.setAdapter(adapter);

        // 초기 요일은 "오늘"
        currentDisplayDow = getTodayDowMon1ToSun7();

        String parentId = prefs.getString(KEY_PARENT_ID, null);
        if (parentId == null || parentId.isEmpty())
            parentId = prefs.getString(KEY_PARENT_ID_FALLBACK, null);

        if (parentId == null || parentId.isEmpty()) {
            Toast.makeText(this, "학부모 로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchChildrenAndBind(parentId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_student_timetable, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_pick_date) {
            showDatePickerAndApplyDow();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchChildrenAndBind(String parentId) {
        parentApi.getChildrenByParentId(parentId).enqueue(new Callback<List<Student>>() {
            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    spinnerChildren.setEnabled(false);
                    spinnerChildren.setAdapter(new NoticeSpinnerAdapter(new ArrayList<ChildItem>() {{
                        add(new ChildItem("", "자녀 없음"));
                    }}));
                    return;
                }

                List<Student> students = response.body();
                List<ChildItem> items = new ArrayList<>();
                if (students != null) {
                    for (Student s : students) {
                        if (s == null) continue;
                        String id = s.getStudentId();
                        String name = s.getStudentName();
                        if (id != null && !id.isEmpty()) {
                            items.add(new ChildItem(id, (name == null || name.isEmpty()) ? id : name));
                        }
                    }
                }

                if (items.isEmpty()) {
                    spinnerChildren.setEnabled(false);
                    spinnerChildren.setAdapter(new NoticeSpinnerAdapter(new ArrayList<ChildItem>() {{
                        add(new ChildItem("", "자녀 없음"));
                    }}));
                    return;
                }

                NoticeSpinnerAdapter adapterSpinner = new NoticeSpinnerAdapter(items);
                spinnerChildren.setAdapter(adapterSpinner);
                spinnerChildren.setEnabled(true);

                String lastSelectedId = prefs.getString(KEY_LAST_SELECTED, null);
                if (lastSelectedId != null) {
                    int pos = findPositionById(items, lastSelectedId);
                    if (pos >= 0) spinnerChildren.setSelection(pos);
                }

                spinnerChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        ChildItem it = (ChildItem) parent.getItemAtPosition(position);
                        if (it.id != null && !it.id.isEmpty()) {
                            prefs.edit().putString(KEY_LAST_SELECTED, it.id).apply();
                            onSelectChild(it.id);
                        }
                    }
                    @Override public void onNothingSelected(AdapterView<?> parent) {}
                });

                spinnerChildren.post(() -> {
                    ChildItem initial = (ChildItem) spinnerChildren.getSelectedItem();
                    if (initial != null && initial.id != null && !initial.id.isEmpty()) {
                        onSelectChild(initial.id);
                    }
                });
            }

            @Override
            public void onFailure(Call<List<Student>> call, Throwable t) {
                spinnerChildren.setEnabled(false);
                spinnerChildren.setAdapter(new NoticeSpinnerAdapter(new ArrayList<ChildItem>() {{
                    add(new ChildItem("", "자녀 없음"));
                }}));
            }
        });
    }

    private void onSelectChild(String studentId) {
        activeChildId = studentId;

        // 이전 네트워크 콜 취소
        if (callClasses != null) callClasses.cancel();
        if (callAttendance != null) callAttendance.cancel();

        // 이전 상태 정리
        adapter.setAll(new ArrayList<>());
        adapter.setClassDowMap(new HashMap<>());
        adapter.setDisplayDow(currentDisplayDow);
        clearSummary();

        // 새 로딩
        fetchClassesThenAttendance(studentId);
    }

    /** 자녀의 수업 목록으로 (정규화된) className→daysOfWeek 맵 구성 후, 출석 전체 로드 */
    private void fetchClassesThenAttendance(String studentId) {
        callClasses = studentApi.getMyClasses(studentId);
        callClasses.enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                if (call.isCanceled() || !studentId.equals(activeChildId)) return;

                classDowMap.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (Course c : response.body()) {
                        if (c == null) continue;
                        String key = norm(c.getClassName());
                        List<Integer> dows = c.getDaysOfWeek(); // 1=월 … 7=일
                        if (!key.isEmpty() && dows != null && !dows.isEmpty()) {
                            classDowMap.put(key, dows);
                        }
                    }
                }
                adapter.setClassDowMap(classDowMap);
                adapter.setDisplayDow(currentDisplayDow);
                fetchAttendanceFromServer(studentId);
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                if (call.isCanceled() || !studentId.equals(activeChildId)) return;

                adapter.setClassDowMap(classDowMap);
                adapter.setDisplayDow(currentDisplayDow);
                fetchAttendanceFromServer(studentId);
            }
        });
    }

    private void fetchAttendanceFromServer(String studentId) {
        callAttendance = studentApi.getAttendanceForStudent(studentId);
        callAttendance.enqueue(new Callback<List<AttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> response) {
                if (call.isCanceled() || !studentId.equals(activeChildId)) return;

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ParentAttendanceActivity.this, "출석 조회 실패(" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    adapter.setAll(new ArrayList<>());
                    clearSummary();
                    return;
                }

                List<AttendanceResponse> list = response.body();
                if (list == null) list = new ArrayList<>();
                try {
                    Collections.sort(list, Comparator.comparing(AttendanceResponse::getDate, String::compareTo));
                } catch (Exception ignore) {}

                allAttendances.clear();
                allAttendances.addAll(list);

                // 리스트 표시(내부 요일 필터)
                adapter.setAll(list);

                // 요약 갱신(현재 선택 요일 기준)
                updateSummaryCountsForDow(currentDisplayDow);
            }

            @Override
            public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                if (call.isCanceled() || !studentId.equals(activeChildId)) return;

                Toast.makeText(ParentAttendanceActivity.this, "출석 조회 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                adapter.setAll(new ArrayList<>());
                clearSummary();
            }
        });

        ThemeColorUtil.applyThemeColor(this, toolbar);
    }

    private void showDatePickerAndApplyDow() {
        Calendar now = Calendar.getInstance(KST, KOR);
        int y = now.get(Calendar.YEAR);
        int m = now.get(Calendar.MONTH);
        int d = now.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar cal = Calendar.getInstance(KST, KOR);
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month); // 0-based
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    int c = cal.get(Calendar.DAY_OF_WEEK); // SUNDAY=1 … SATURDAY=7
                    int dow = (c == Calendar.SUNDAY) ? 7 : (c - 1); // 1=월 … 7=일
                    currentDisplayDow = dow;
                    if (adapter != null) adapter.setDisplayDow(dow);
                    updateSummaryCountsForDow(dow);
                },
                y, m, d
        );
        dlg.show();
    }

    /** 선택된 요일 기준 요약 재계산 */
    private void updateSummaryCountsForDow(int dowMon1ToSun7) {
        if (dowMon1ToSun7 < 1 || dowMon1ToSun7 > 7) {
            updateSummaryCounts(allAttendances);
            return;
        }
        List<AttendanceResponse> filtered = new ArrayList<>();
        for (AttendanceResponse ar : allAttendances) {
            if (ar == null) continue;
            List<Integer> dows = null;
            try { dows = ar.getDaysOfWeek(); } catch (Throwable ignored) {}
            if (dows == null || dows.isEmpty()) {
                String key = norm(ar.getClassName());
                dows = classDowMap.get(key);
            }
            if (dows == null) continue;
            if (dows.contains(dowMon1ToSun7)) filtered.add(ar);
        }
        updateSummaryCounts(filtered);
    }

    /** 전달 목록에서 출석/지각/결석 카운트 후 상단 표시 */
    private void updateSummaryCounts(List<AttendanceResponse> list) {
        long present = 0, late = 0, absent = 0;
        for (AttendanceResponse ar : list) {
            String s = normalizeStatus(ar != null ? ar.getStatus() : null);
            if ("출석".equals(s)) present++;
            else if ("지각".equals(s)) late++;
            else if ("결석".equals(s)) absent++;
        }
        if (tvPresent != null) tvPresent.setText("출석 " + present);
        if (tvLate    != null) tvLate.setText("지각 " + late);
        if (tvAbsent  != null) tvAbsent.setText("결석 " + absent);
    }

    private void clearSummary() {
        if (tvPresent != null) tvPresent.setText("출석 0");
        if (tvLate    != null) tvLate.setText("지각 0");
        if (tvAbsent  != null) tvAbsent.setText("결석 0");
    }

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

    private int getTodayDowMon1ToSun7() {
        Calendar cal = Calendar.getInstance(KST, KOR);
        int c = cal.get(Calendar.DAY_OF_WEEK);
        return (c == Calendar.SUNDAY) ? 7 : (c - 1);
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private int findPositionById(List<ChildItem> list, String id) {
        for (int i = 0; i < list.size(); i++) if (id.equals(list.get(i).id)) return i;
        return -1;
    }

    /** 공지사항과 동일한 스피너 디자인: res/layout/item_spinner_text.xml (TextView @id/tv_title) */
    private class NoticeSpinnerAdapter extends ArrayAdapter<ChildItem> {
        NoticeSpinnerAdapter(List<ChildItem> items) {
            super(ParentAttendanceActivity.this, R.layout.item_spinner_text, items);
            setDropDownViewResource(R.layout.item_spinner_text);
        }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            bind(v, getItem(position));
            return v;
        }
        @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v = super.getDropDownView(position, convertView, parent);
            bind(v, getItem(position));
            return v;
        }
        private void bind(View root, ChildItem item) {
            if (root == null || item == null) return;
            if (root instanceof TextView) ((TextView) root).setText(item.name);
            else {
                TextView tv = root.findViewById(R.id.tv_title);
                if (tv != null) tv.setText(item.name);
            }
        }
    }
}
