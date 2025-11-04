package com.mobile.greenacademypartner.ui.mypage;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.parent.ParentUpdateRequest;
import com.mobile.greenacademypartner.model.student.Student;
import com.mobile.greenacademypartner.model.student.StudentUpdateRequest;
import com.mobile.greenacademypartner.ui.attendance.AttendanceActivity;
import com.mobile.greenacademypartner.ui.main.MainActivity;
import com.mobile.greenacademypartner.ui.qna.QuestionsActivity;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;
import com.mobile.greenacademypartner.ui.timetable.QRScannerActivity;
import com.mobile.greenacademypartner.ui.timetable.StudentTimetableActivity;

import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyPageActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText editName, editId, editPhone, editAddress, editSchool, editGrade, editGender, editAcademyNumber;
    private TextView textRoleTitle;
    private Button btnSave;
    private String role;

    private Spinner spinnerProfileTarget;
    private final List<ProfileItem> targets = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private final Map<String, Student> studentCache = new HashMap<>();
    private ProfileItem.Type currentSelectionType = ProfileItem.Type.SELF;
    private String currentStudentId = null;
    private String parentDisplayName = "";

    // 원래 부모 정보
    private String originalParentName = "";
    private String originalParentPhone = "";

    private BottomNavigationView bottomNavigationView;
    private ImageButton btnShowNav, btnHideNav;

    private static class ProfileItem {
        enum Type { SELF, STUDENT }
        final Type type;
        final String id;
        final String label;
        ProfileItem(Type type, String id, String label) {
            this.type = type; this.id = id; this.label = label;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initViews();

        loadUserInfoAndSetTitles();
        setupUIByRole();
        setupProfileSpinnerIfParent();
        setupSaveButton();
        setupBottomNavigation();

        ThemeColorUtil.applyThemeColor(this, toolbar);
    }

    private void initViews() {
        textRoleTitle = findViewById(R.id.text_role_title);
        editName = findViewById(R.id.edit_name);
        editId = findViewById(R.id.edit_id);
        editPhone = findViewById(R.id.edit_phone);
        editAddress = findViewById(R.id.edit_address);
        editSchool = findViewById(R.id.edit_school);
        editGrade = findViewById(R.id.edit_grade);
        editGender = findViewById(R.id.edit_gender);
        editAcademyNumber = findViewById(R.id.edit_academy_number);
        btnSave = findViewById(R.id.btn_save);
        spinnerProfileTarget = findViewById(R.id.spinner_profile_target);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnShowNav = findViewById(R.id.btn_show_nav);
        btnHideNav = findViewById(R.id.btn_hide_nav);
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) startActivity(new Intent(this, MainActivity.class));
                else if (id == R.id.nav_attendance) startActivity(new Intent(this, AttendanceActivity.class));
                else if (id == R.id.nav_qr) startActivity(new Intent(this, QRScannerActivity.class));
                else if (id == R.id.nav_timetable) startActivity(new Intent(this, StudentTimetableActivity.class));
                else if (id == R.id.nav_my) return true;
                return true;
            });
            bottomNavigationView.setSelectedItemId(R.id.nav_my);
        }

        if (btnShowNav != null && btnHideNav != null) {
            btnHideNav.setOnClickListener(v -> {
                bottomNavigationView.setVisibility(View.GONE);
                btnHideNav.setVisibility(View.GONE);
                btnShowNav.setVisibility(View.VISIBLE);
            });
            btnShowNav.setOnClickListener(v -> {
                bottomNavigationView.setVisibility(View.VISIBLE);
                btnHideNav.setVisibility(View.VISIBLE);
                btnShowNav.setVisibility(View.GONE);
            });
        }
    }

    private void loadUserInfoAndSetTitles() {
        SharedPreferences pref = getSharedPreferences("login_prefs", MODE_PRIVATE);

        role = pref.getString("role", "unknown").trim().toLowerCase();

        String name = pref.getString("name", "");
        String username = pref.getString("username", "");
        String phone = pref.getString("phone", "");

        editName.setText(name);
        editId.setText(username);
        editPhone.setText(phone);

        // 원래 값 저장
        originalParentName = name;
        originalParentPhone = phone;

        if ("student".equals(role)) {
            editAddress.setText(pref.getString("address", ""));
            editSchool.setText(pref.getString("school", ""));
            int grade = pref.getInt("grade", 0);
            editGrade.setText(grade == 0 ? "" : String.valueOf(grade));
            editGender.setText(pref.getString("gender", ""));
        }

        String titleName = (name != null && !name.trim().isEmpty()) ? name.trim() : "사용자";
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(titleName + "의 마이페이지");
        }
        parentDisplayName = titleName;
        updateCardTitle(parentDisplayName);
    }

    private void updateCardTitle(String displayName) {
        String who = (displayName == null || displayName.trim().isEmpty()) ? "사용자" : displayName.trim();
        textRoleTitle.setText(who + "의 마이페이지");
    }

    private void setupUIByRole() {
        editAddress.setVisibility(View.GONE);
        editSchool.setVisibility(View.GONE);
        editGrade.setVisibility(View.GONE);
        editGender.setVisibility(View.GONE);
        editAcademyNumber.setVisibility(View.GONE);

        if ("student".equals(role)) {
            editAddress.setVisibility(View.VISIBLE);
            editSchool.setVisibility(View.VISIBLE);
            editGrade.setVisibility(View.VISIBLE);
            editGender.setVisibility(View.VISIBLE);
            lockStudentFields();
        } else if ("teacher".equals(role)) {
            editAcademyNumber.setVisibility(View.VISIBLE);
        } else if ("parent".equals(role)) {
            setFormEnabled(true);
            setSaveEnabled(true);
            showStudentOnlyFields(false);
        }
    }

    private void setupProfileSpinnerIfParent() {
        if (!"parent".equals(role)) {
            if (spinnerProfileTarget != null) spinnerProfileTarget.setVisibility(View.GONE);
            return;
        }
        if (spinnerProfileTarget == null) return;

        spinnerProfileTarget.setVisibility(View.VISIBLE);

        SharedPreferences pref = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String parentId = pref.getString("parentId", pref.getString("username", ""));
        String parentName = pref.getString("name", "");
        targets.clear();
        targets.add(new ProfileItem(ProfileItem.Type.SELF, parentId, "나(" + parentName + ")"));

        List<String> labels = new ArrayList<>();
        for (ProfileItem it : targets) labels.add(it.label);
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProfileTarget.setAdapter(spinnerAdapter);

        int last = pref.getInt("mypage_selected_index", 0);
        if (last >= 0 && last < targets.size()) {
            spinnerProfileTarget.setSelection(last);
            currentSelectionType = targets.get(last).type;
        } else {
            currentSelectionType = ProfileItem.Type.SELF;
        }
        updateSaveButtonState();

        loadChildrenAsync(parentId);

        spinnerProfileTarget.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                getSharedPreferences("login_prefs", MODE_PRIVATE)
                        .edit().putInt("mypage_selected_index", pos).apply();

                ProfileItem sel = targets.get(pos);
                currentSelectionType = sel.type;

                if (sel.type == ProfileItem.Type.SELF) {
                    currentStudentId = null;
                    updateCardTitle(parentDisplayName);
                    bindParentFromPrefs();
                    setFormEnabled(true);
                    editId.setEnabled(true);
                    setSaveEnabled(true);
                    showStudentOnlyFields(false);
                } else {
                    currentStudentId = sel.id;
                    Student cached = studentCache.get(sel.id);
                    if (cached != null) {
                        updateCardTitle(safe(cached.getStudentName()));
                        editId.setText(safe(cached.getStudentId()));
                        editName.setText(safe(cached.getStudentName()));
                    } else {
                        updateCardTitle(sel.label);
                        editId.setText(sel.id);
                    }
                    loadStudentDetailAsync(sel.id);
                    setFormEnabled(true);
                    editId.setEnabled(false);
                    setSaveEnabled(true);
                    showStudentOnlyFields(true);
                }
                updateSaveButtonState();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        bindParentFromPrefs();
        updateCardTitle(parentDisplayName);
    }

    private void loadChildrenAsync(String parentId) {
        try {
            ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
            Call<List<Student>> call = api.getChildrenByParentId(parentId);
            call.enqueue(new Callback<List<Student>>() {
                @Override
                public void onResponse(Call<List<Student>> call, Response<List<Student>> resp) {
                    if (!resp.isSuccessful() || resp.body() == null) return;
                    for (Student s : resp.body()) {
                        if (s == null) continue;
                        String sid = safe(s.getStudentId());
                        String sname = safe(s.getStudentName());
                        if (sid.isEmpty() || sname.isEmpty()) continue;
                        studentCache.put(sid, s);
                        targets.add(new ProfileItem(ProfileItem.Type.STUDENT, sid, sname));
                    }
                    List<String> labels = new ArrayList<>();
                    for (ProfileItem it : targets) labels.add(it.label);
                    spinnerAdapter.clear();
                    spinnerAdapter.addAll(labels);
                    spinnerAdapter.notifyDataSetChanged();
                }
                @Override public void onFailure(Call<List<Student>> call, Throwable t) {}
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadStudentDetailAsync(String studentId) {
        try {
            StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
            Call<Student> call = api.getStudentById(studentId);
            call.enqueue(new Callback<Student>() {
                @Override
                public void onResponse(Call<Student> call, Response<Student> resp) {
                    if (!resp.isSuccessful() || resp.body() == null) return;
                    Student s = resp.body();
                    if (s.getStudentId() != null) studentCache.put(s.getStudentId(), s);
                    if (currentSelectionType == ProfileItem.Type.STUDENT &&
                            currentStudentId != null &&
                            currentStudentId.equals(s.getStudentId())) {
                        updateCardTitle(safe(s.getStudentName()));
                        bindStudentToForm(s);
                    }
                }
                @Override public void onFailure(Call<Student> call, Throwable t) {}
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String safe(String v) { return v == null ? "" : v; }

    private void setupSaveButton() {
        if (btnSave == null) return;

        btnSave.setOnClickListener(v -> {
            if ("student".equals(role)) {
                Toast.makeText(this, "학생 계정으로는 수정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1) 우선 화면 값 전부 읽어서 “한 번에” 정리
            String rawId = safe(editId.getText().toString());
            String rawName = safe(editName.getText().toString());
            String rawPhone = safe(editPhone.getText().toString());
            String rawAddress = safe(editAddress.getText().toString());
            String rawSchool = safe(editSchool.getText().toString());
            String rawGender = safe(editGender.getText().toString());
            int gradeTmp;
            try { gradeTmp = Integer.parseInt(safe(editGrade.getText().toString()).trim()); }
            catch (Exception e) { gradeTmp = 0; }

            // 2) 여기서 한 번만 백업값 대입
            if (rawName == null || rawName.trim().isEmpty()) {
                rawName = originalParentName;
            }
            if (rawPhone == null || rawPhone.trim().isEmpty()) {
                rawPhone = originalParentPhone;
            }

            // 3) 이 시점의 값들을 전부 final 로 만들어서 콜백에서 쓸 거야
            final String fixedId = rawId;
            final String fixedName = rawName;
            final String fixedPhone = rawPhone;
            final String fixedAddress = rawAddress;
            final String fixedSchool = rawSchool;
            final String fixedGender = rawGender;
            final int fixedGrade = gradeTmp;

            SharedPreferences pref = getSharedPreferences("login_prefs", MODE_PRIVATE);
            final SharedPreferences sharedPrefFinal = pref; // 콜백에서 쓰려고

            String token = pref.getString("token", "");

            if ("parent".equals(role)) {
                // ▶ 부모 자기 정보
                if (currentSelectionType == ProfileItem.Type.SELF) {

                    ParentUpdateRequest parent = new ParentUpdateRequest(fixedId, fixedName, fixedPhone);
                    ParentApi api = RetrofitClient.getClient().create(ParentApi.class);

                    api.updateParent("Bearer " + token, fixedId, parent)
                            .enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    boolean ok = response.isSuccessful();
                                    showToast(ok, "학부모");
                                    if (ok) {
                                        sharedPrefFinal.edit()
                                                .putString("parentId", fixedId)
                                                .putString("username", fixedId)
                                                .putString("name", fixedName)
                                                .putString("phone", fixedPhone)
                                                .apply();

                                        // 원본 갱신
                                        originalParentName = fixedName;
                                        originalParentPhone = fixedPhone;

                                        loadUserInfoAndSetTitles();
                                        updateCardTitle(parentDisplayName);
                                    } else {
                                        Log.e("MyPage", "updateParent 실패 code=" + response.code());
                                    }
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Log.e("MyPage", "updateParent 실패", t);
                                    showToast(false, "학부모");
                                }
                            });

                }
                // ▶ 자녀 정보
                else if (currentSelectionType == ProfileItem.Type.STUDENT && currentStudentId != null) {

                    final String fixedStudentId = currentStudentId;
                    final String parentId = pref.getString("parentId", pref.getString("username", ""));

                    StudentUpdateRequest req = new StudentUpdateRequest(
                            fixedStudentId,
                            fixedName,
                            fixedPhone,
                            fixedAddress,
                            fixedSchool,
                            fixedGrade,
                            fixedGender
                    );
                    req.setParentId(parentId);

                    StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
                    api.updateStudent("Bearer " + token, fixedStudentId, req)
                            .enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    boolean ok = response.isSuccessful();
                                    showToast(ok, "학생");
                                    if (ok) {
                                        Student cached = studentCache.get(fixedStudentId);
                                        if (cached != null) {
                                            cached.setStudentName(fixedName);
                                            cached.setStudentPhoneNumber(fixedPhone);
                                            cached.setStudentAddress(fixedAddress);
                                            cached.setSchool(fixedSchool);
                                            cached.setGrade(fixedGrade);
                                            cached.setGender(fixedGender);
                                        }
                                        sharedPrefFinal.edit()
                                                .putString("last_student_id", fixedStudentId)
                                                .putString("last_student_name", fixedName)
                                                .apply();
                                        updateCardTitle(fixedName);
                                    } else {
                                        Log.e("MyPage", "updateStudent 실패 code=" + response.code());
                                    }
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Log.e("MyPage", "updateStudent 실패", t);
                                    showToast(false, "학생");
                                }
                            });
                }
            }
        });

        updateSaveButtonState();
    }

    private void updateSaveButtonState() {
        if (btnSave == null) return;
        boolean enabled;
        if ("parent".equals(role)) {
            enabled = (currentSelectionType == ProfileItem.Type.SELF || currentSelectionType == ProfileItem.Type.STUDENT);
        } else if ("student".equals(role)) {
            enabled = false;
        } else {
            enabled = (currentSelectionType == ProfileItem.Type.SELF);
        }
        setSaveEnabled(enabled);
    }

    private void showToast(boolean success, String roleName) {
        String msg = roleName + (success ? " 정보가 성공적으로 수정되었습니다." : " 정보 수정에 실패했습니다.");
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void lockStudentFields() {
        makeReadOnly(editId);
        makeReadOnly(editName);
        makeReadOnly(editPhone);
        makeReadOnly(editAddress);
        makeReadOnly(editSchool);
        makeReadOnly(editGrade);
        makeReadOnly(editGender);

        tintGray(editId); tintGray(editName); tintGray(editPhone);
        tintGray(editAddress); tintGray(editSchool); tintGray(editGrade); tintGray(editGender);

        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setVisibility(View.GONE);
        }
    }

    private void makeReadOnly(EditText et) {
        if (et == null) return;
        et.setFocusable(false);
        et.setFocusableInTouchMode(false);
        et.setClickable(false);
        et.setLongClickable(false);
        et.setCursorVisible(false);
        et.setTextIsSelectable(true);
        et.setKeyListener(null);
    }

    private void tintGray(EditText et) {
        if (et == null) return;
        et.setTextColor(Color.parseColor("#6B7280"));
        et.setHintTextColor(Color.parseColor("#9AA0A6"));
    }

    private void bindParentFromPrefs() {
        SharedPreferences pref = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String name = pref.getString("name", "");
        String username = pref.getString("username", "");
        String phone = pref.getString("phone", "");

        editName.setText(name);
        editId.setText(username);
        editPhone.setText(phone);

        // 이때도 원본 갱신
        originalParentName = name;
        originalParentPhone = phone;

        editAddress.setText(""); editSchool.setText(""); editGrade.setText(""); editGender.setText("");
    }

    private void bindStudentToForm(Student s) {
        if (s == null) return;
        editName.setText(safe(s.getStudentName()));
        editId.setText(safe(s.getStudentId()));
        editPhone.setText(safe(s.getStudentPhoneNumber()));
        editAddress.setText(safe(s.getStudentAddress()));
        editSchool.setText(safe(s.getSchool()));
        try {
            int g = s.getGrade();
            editGrade.setText(g == 0 ? "" : String.valueOf(g));
        } catch (Exception e) {
            editGrade.setText("");
        }
        editGender.setText(safe(s.getGender()));
    }

    private void setFormEnabled(boolean enabled) {
        EditText[] arr = new EditText[]{ editName, editId, editPhone, editAddress, editSchool, editGrade, editGender };
        for (EditText et : arr) { if (et != null) et.setEnabled(enabled); }
    }

    private void setSaveEnabled(boolean enabled) {
        if (btnSave == null) return;
        btnSave.setEnabled(enabled);
        btnSave.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    private void setRowVisibility(EditText et, int visibility) {
        if (et == null) return;
        et.setVisibility(visibility);
        View p1 = (View) et.getParent();
        if (p1 != null) {
            p1.setVisibility(visibility);
            View p2 = (View) p1.getParent();
            if (p2 != null) p2.setVisibility(visibility);
        }
    }

    private void showStudentOnlyFields(boolean show) {
        int v = show ? View.VISIBLE : View.GONE;
        setRowVisibility(editAddress, v);
        setRowVisibility(editSchool, v);
        setRowVisibility(editGrade, v);
        setRowVisibility(editGender, v);
        if (!show) {
            if (editAddress != null) editAddress.setText("");
            if (editSchool  != null) editSchool.setText("");
            if (editGrade   != null) editGrade.setText("");
            if (editGender  != null) editGender.setText("");
        }
    }
}
