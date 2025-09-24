package com.mobile.greenacademypartner.ui.timetable;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.mobile.greenacademypartner.model.classes.Course;
import com.mobile.greenacademypartner.model.student.Student;
import com.mobile.greenacademypartner.ui.adapter.TimetableAdapter;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParentChildrenListActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private Spinner spinnerChildren;
    private RecyclerView rvTimetable;

    private SharedPreferences prefs;
    private ParentApi parentApi;
    private StudentApi studentApi;

    private TimetableAdapter adapter;

    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_PARENT_ID = "parentId";
    private static final String KEY_PARENT_ID_FALLBACK = "username";
    private static final String KEY_LAST_SELECTED = "last_selected_child_id_tm";

    // 타임존/로케일/표시일 포맷
    private final TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
    private final Locale   loc = Locale.KOREA;
    private final java.text.SimpleDateFormat iso =
            new java.text.SimpleDateFormat("yyyy-MM-dd", loc);

    // 현재 선택된 날짜/요일 상태(달력 변경 시 유지)
    private String currentDateIso;
    private int currentDowMon1to7; // 1=월 … 7=일

    private static class ChildItem {
        final String id; final String name;
        ChildItem(String id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    private class NoticeSpinnerAdapter extends ArrayAdapter<ChildItem> {
        NoticeSpinnerAdapter(List<ChildItem> items) {
            super(ParentChildrenListActivity.this, R.layout.item_spinner_text, items);
            setDropDownViewResource(R.layout.item_spinner_text);
        }
        @Override public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
            android.view.View v = super.getView(position, convertView, parent);
            bind(v, getItem(position)); return v;
        }
        @Override public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
            android.view.View v = super.getDropDownView(position, convertView, parent);
            bind(v, getItem(position)); return v;
        }
        private void bind(android.view.View root, ChildItem item) {
            if (root == null || item == null) return;
            if (root instanceof TextView) ((TextView) root).setText(item.name);
            else {
                TextView tv = root.findViewById(R.id.tv_title);
                if (tv != null) tv.setText(item.name);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        iso.setTimeZone(tz);
        setContentView(R.layout.activity_children_list); // 파일명 유지

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("자녀 시간표");
        int white = ContextCompat.getColor(this, android.R.color.white);
        toolbar.setTitleTextColor(white);

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        ThemeColorUtil.applyThemeColor(this, toolbar);
        NavigationMenuHelper.setupMenu(this, findViewById(R.id.nav_container), drawerLayout, null, 2);

        spinnerChildren = findViewById(R.id.spinner_children);
        rvTimetable     = findViewById(R.id.rv_timetable);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        parentApi  = RetrofitClient.getClient().create(ParentApi.class);
        studentApi = RetrofitClient.getClient().create(StudentApi.class);

        rvTimetable.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimetableAdapter(this, new ArrayList<>());
        rvTimetable.setAdapter(adapter);

        // 초기 날짜/요일은 "오늘"
        currentDateIso = todayIso();
        currentDowMon1to7 = todayDow1to7();

        // parentId 확보
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
        // 학생 시간표와 동일한 달력 메뉴 사용
        getMenuInflater().inflate(R.menu.menu_student_timetable, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_pick_date) {
            showDatePickerAndReload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchChildrenAndBind(String parentId) {
        parentApi.getChildrenByParentId(parentId).enqueue(new Callback<List<Student>>() {
            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {
                if (!response.isSuccessful() || response.body() == null) { bindEmptyChildren(); return; }

                List<ChildItem> items = new ArrayList<>();
                for (Student s : response.body()) {
                    if (s == null) continue;
                    String id = s.getStudentId();
                    String nm = s.getStudentName();
                    if (id != null && !id.isEmpty()) items.add(new ChildItem(id, (nm == null || nm.isEmpty()) ? id : nm));
                }
                if (items.isEmpty()) { bindEmptyChildren(); return; }

                NoticeSpinnerAdapter ad = new NoticeSpinnerAdapter(items);
                spinnerChildren.setAdapter(ad);
                spinnerChildren.setEnabled(true);

                String lastId = prefs.getString(KEY_LAST_SELECTED, null);
                if (lastId != null) {
                    int pos = findChildPos(items, lastId);
                    if (pos >= 0) spinnerChildren.setSelection(pos);
                }

                spinnerChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                        ChildItem it = (ChildItem) parent.getItemAtPosition(position);
                        if (it.id != null && !it.id.isEmpty()) {
                            prefs.edit().putString(KEY_LAST_SELECTED, it.id).apply();
                            // 현재 선택된 날짜/요일로 로드
                            loadTimetableFor(it.id, currentDowMon1to7, currentDateIso);
                        }
                    }
                    @Override public void onNothingSelected(AdapterView<?> parent) {}
                });

                // 초기에 선택된 자녀로 로드
                spinnerChildren.post(() -> {
                    ChildItem it = (ChildItem) spinnerChildren.getSelectedItem();
                    if (it != null && it.id != null && !it.id.isEmpty()) {
                        loadTimetableFor(it.id, currentDowMon1to7, currentDateIso);
                    }
                });
            }
            @Override public void onFailure(Call<List<Student>> call, Throwable t) { bindEmptyChildren(); }
        });
    }

    private void bindEmptyChildren() {
        List<ChildItem> empty = new ArrayList<>();
        empty.add(new ChildItem("", "자녀 없음"));
        spinnerChildren.setAdapter(new NoticeSpinnerAdapter(empty));
        spinnerChildren.setEnabled(false);
        adapter.submit(new ArrayList<>());
        adapter.setDisplayDate(currentDateIso != null ? currentDateIso : todayIso());
    }

    private int findChildPos(List<ChildItem> list, String id) {
        for (int i = 0; i < list.size(); i++) if (id.equals(list.get(i).id)) return i;
        return -1;
    }

    /** 선택된 날짜/요일 기준으로 시간표 로드(학생 화면과 동일한 요일 필터) */
    private void loadTimetableFor(String studentId, int dowMon1to7, String dateIso) {
        studentApi.getMyClasses(studentId).enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    adapter.submit(new ArrayList<>());
                    adapter.setDisplayDate(dateIso);
                    Toast.makeText(ParentChildrenListActivity.this, "시간표 조회 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Course> all = response.body();
                List<Course> filtered = new ArrayList<>();
                if (all != null) {
                    for (Course c : all) {
                        if (c == null || c.getDaysOfWeek() == null) continue;
                        if (c.getDaysOfWeek().contains(dowMon1to7)) filtered.add(c);
                    }
                    filtered.sort(Comparator.comparing(
                            v -> v.getStartTime() != null ? v.getStartTime() : "",
                            String::compareTo
                    ));
                }

                adapter.submit(filtered);
                adapter.setDisplayDate(dateIso);

                if (filtered.isEmpty()) {
                    Toast.makeText(ParentChildrenListActivity.this, "해당 요일에 예정된 수업이 없습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                adapter.submit(new ArrayList<>());
                adapter.setDisplayDate(dateIso);
                Toast.makeText(ParentChildrenListActivity.this, "시간표 조회 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePickerAndReload() {
        Calendar now = Calendar.getInstance(tz, loc);
        int y = now.get(Calendar.YEAR);
        int m = now.get(Calendar.MONTH);
        int d = now.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar pick = Calendar.getInstance(tz, loc);
                    pick.set(Calendar.YEAR, year);
                    pick.set(Calendar.MONTH, month); // 0-based
                    pick.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    int dowJavaUtil = pick.get(Calendar.DAY_OF_WEEK); // 일=1 … 토=7
                    int dowMon1 = (dowJavaUtil == Calendar.SUNDAY) ? 7 : (dowJavaUtil - 1);

                    currentDateIso = iso.format(pick.getTime());
                    currentDowMon1to7 = dowMon1;

                    ChildItem sel = (ChildItem) spinnerChildren.getSelectedItem();
                    if (sel != null && sel.id != null && !sel.id.isEmpty()) {
                        loadTimetableFor(sel.id, currentDowMon1to7, currentDateIso);
                    }
                },
                y, m, d
        );
        dlg.show();
    }

    private String todayIso() {
        Calendar cal = Calendar.getInstance(tz, loc);
        return iso.format(cal.getTime());
    }

    /** Calendar.DAY_OF_WEEK(일=1…토=7) → 규약(월=1…일=7) */
    private int todayDow1to7() {
        Calendar cal = Calendar.getInstance(tz, loc);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        return dow == Calendar.SUNDAY ? 7 : (dow - 1);
    }
}
