package com.mobile.greenacademypartner.ui.mypage;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.StudentApi;

// import com.mobile.greenacademypartner.api.TeacherApi;              // 🔕 [DISABLED] teacher API 제거

import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.menu.ToolbarIconUtil;
import com.mobile.greenacademypartner.model.parent.ParentUpdateRequest;
import com.mobile.greenacademypartner.model.student.Student;
import com.mobile.greenacademypartner.model.student.StudentUpdateRequest;

// import com.mobile.greenacademypartner.model.teacher.TeacherUpdateRequest; // 🔕 [DISABLED] teacher 모델 제거

import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyPageActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private LinearLayout navContainer;
    private Toolbar toolbar;
    private EditText editName, editId, editPhone, editAddress, editSchool, editGrade, editGender, editAcademyNumber;
    private TextView textRoleTitle;
    private Button btnSave;
    private String role;

    // ▼ 스피너(나/자녀 전환) 관련
    private Spinner spinnerProfileTarget;
    private final List<ProfileItem> targets = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private final Map<String, Student> studentCache = new HashMap<>();
    private ProfileItem.Type currentSelectionType = ProfileItem.Type.SELF; // 기본값
    private String currentStudentId = null; // 현재 선택된 학생 ID(저장 시 사용)

    // ▼ 카드 제목 동기화를 위한 학부모 표시 이름(툴바는 로그인 사용자 기준 유지)
    private String parentDisplayName = "";

    private static class ProfileItem {
        enum Type { SELF, STUDENT }
        final Type type;
        final String id;
        final String label;
        ProfileItem(Type type, String id, String label) {
            this.type = type; this.id = id; this.label = label;
        }
    }

    private final int defaultIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        // Drawer & Toolbar 설정
        drawerLayout = findViewById(R.id.drawer_layout);
        navContainer = findViewById(R.id.nav_container);
        toolbar = findViewById(R.id.toolbar);
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        ToolbarIconUtil.applyWhiteIcons(toolbar, toggle);

        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, null, defaultIndex);

        // 주요 구성요소 초기화
        initViews();


        // 사용자 정보 로드 및 제목 반영
        loadUserInfoAndSetTitles();

        // 역할별 기본 UI 표시/숨김 (학생 로그인 시 읽기 전용 잠금)
        setupUIByRole();

        // (학부모일 때만) 스피너로 '나/자녀' 전환 구성
        setupProfileSpinnerIfParent();

        // 저장 버튼 클릭
        setupSaveButton();


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
        editAcademyNumber = findViewById(R.id.edit_academy_number); // teacher용 (숨김 처리)
        btnSave = findViewById(R.id.btn_save);

        // 스피너(레이아웃에 추가되어 있어야 함: @id/spinner_profile_target)
        spinnerProfileTarget = findViewById(R.id.spinner_profile_target);
    }

    /**
     * SharedPreferences에서 사용자 정보를 읽고,
     * 툴바/헤더에 "OOO의 마이페이지"로 제목을 반영합니다.
     * 하단 카드 제목은 스피너 선택에 따라 동적으로 바뀌므로, 여기서는 학부모 이름만 저장해 둡니다.
     */
    private void loadUserInfoAndSetTitles() {
        SharedPreferences pref = getSharedPreferences("login_prefs", MODE_PRIVATE);

        role = pref.getString("role", "unknown").trim().toLowerCase();

        String name = pref.getString("name", "");
        String username = pref.getString("username", "");
        String phone = pref.getString("phone", "");

        editName.setText(name);
        editId.setText(username);
        editPhone.setText(phone);

        if ("student".equals(role)) {
            editAddress.setText(pref.getString("address", ""));
            editSchool.setText(pref.getString("school", ""));
            int grade = pref.getInt("grade", 0);
            editGrade.setText(grade == 0 ? "" : String.valueOf(grade));
            editGender.setText(pref.getString("gender", ""));
        }
        // 🔕 [DISABLED] teacher 전용 필드 로드
        // else if ("teacher".equals(role)) {
        //     editAcademyNumber.setText(String.valueOf(pref.getInt("academyNumber", 0)));
        // }


        // 툴바 제목(로그인 사용자 기준)
        String titleName = (name != null && !name.trim().isEmpty()) ? name.trim() : "사용자";
        String fullTitle = titleName + "의 마이페이지";
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fullTitle);
        }

        // 학부모 기준 디폴트 카드 제목 세팅(스피너 선택 시 갱신됨)
        parentDisplayName = titleName;
        updateCardTitle(parentDisplayName);

        // 전체 프리퍼런스 로그(디버그)
        Map<String, ?> allEntries = pref.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("MyPage", entry.getKey() + ": " + entry.getValue());
        }
    }

    /** 카드 제목을 "[이름]의 마이페이지"로 갱신 */
    private void updateCardTitle(String displayName) {
        String who = (displayName == null || displayName.trim().isEmpty()) ? "사용자" : displayName.trim();
        textRoleTitle.setText(who + "의 마이페이지");
    }

    /**
     * 역할별로 보일 입력 필드만 표시/숨김합니다.
     * 학생 로그인은 모든 필드를 읽기전용으로 만들고 저장 버튼도 비활성화/숨김합니다.
     * (학부모 로그인 + 스피너 전환 시에는 lock 함수를 쓰지 않고 setEnabled 토글로 처리)
     */
    private void setupUIByRole() {

        editAddress.setVisibility(View.GONE);
        editSchool.setVisibility(View.GONE);
        editGrade.setVisibility(View.GONE);
        editGender.setVisibility(View.GONE);

        editAcademyNumber.setVisibility(View.GONE);


        if ("student".equals(role)) {
            // 표시만 하고, 아래에서 전부 읽기전용으로 잠금
            editAddress.setVisibility(View.VISIBLE);
            editSchool.setVisibility(View.VISIBLE);
            editGrade.setVisibility(View.VISIBLE);
            editGender.setVisibility(View.VISIBLE);


            // 🔕 [DISABLED] teacher 화면
            // } else if ("teacher".equals(role)) {
            //     textRoleTitle.setText("교사 마이페이지");
            //     editAcademyNumber.setVisibility(View.VISIBLE);


            lockStudentFields(); // ★ 학생 로그인은 모든 필드를 영구 잠금
        } else if ("teacher".equals(role)) {
            editAcademyNumber.setVisibility(View.VISIBLE);

        } else if ("parent".equals(role)) {
            // 기본은 '나(학부모)' 폼 편집 가능 + 학생 전용 필드 숨김
            setFormEnabled(true);
            setSaveEnabled(true);
            showStudentOnlyFields(false);
        } else {
            // unknown
        }
    }



    private void setupProfileSpinnerIfParent() {
        if (!"parent".equals(role)) {
            // 학부모가 아니면 스피너 자체를 숨김(레이아웃에 있는 경우)
            if (spinnerProfileTarget != null) spinnerProfileTarget.setVisibility(View.GONE);
            return;
        }
        if (spinnerProfileTarget == null) return;


        spinnerProfileTarget.setVisibility(View.VISIBLE);

        // 1) 기본: 본인(학부모) 항목 추가
        SharedPreferences pref = getSharedPreferences("login_prefs", MODE_PRIVATE);
        // parentId 우선 사용, 없으면 username 대체
        String parentId = pref.getString("parentId", pref.getString("username", ""));
        String parentName = pref.getString("name", "");
        targets.clear();
        targets.add(new ProfileItem(ProfileItem.Type.SELF, parentId, "나(" + parentName + ")"));


        // 어댑터(라벨만)
        List<String> labels = new ArrayList<>();
        for (ProfileItem it : targets) labels.add(it.label);
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProfileTarget.setAdapter(spinnerAdapter);


        // 마지막 선택 복원(없으면 0=본인)
        int last = pref.getInt("mypage_selected_index", 0);
        if (last >= 0 && last < targets.size()) {
            spinnerProfileTarget.setSelection(last);
            currentSelectionType = targets.get(last).type;
        } else {
            currentSelectionType = ProfileItem.Type.SELF;
        }
        updateSaveButtonState();

        // 2) 자녀 목록 비동기 로딩 → 스피너에 추가 (ParentApi 사용, /api/parents/{parentId}/children)
        loadChildrenAsync(parentId);

        // 3) 선택 이벤트
        spinnerProfileTarget.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                getSharedPreferences("login_prefs", MODE_PRIVATE)
                        .edit().putInt("mypage_selected_index", pos).apply();


                ProfileItem sel = targets.get(pos);
                currentSelectionType = sel.type;

                if (sel.type == ProfileItem.Type.SELF) {
                    currentStudentId = null;

                    // 카드 제목: 학부모 이름
                    updateCardTitle(parentDisplayName);

                    // 학부모 정보 표시(편집 가능)
                    bindParentFromPrefs();
                    setFormEnabled(true);
                    editId.setEnabled(true); // 학생 선택 시 잠갔던 ID 재활성화
                    setSaveEnabled(true);
                    showStudentOnlyFields(false);
                } else {
                    currentStudentId = sel.id;

                    // 캐시에 있으면 최소 정보(ID/이름) 즉시 표시 + 카드 제목 선반영
                    Student cached = studentCache.get(sel.id);
                    if (cached != null) {
                        updateCardTitle(safe(cached.getStudentName()));
                        editId.setText(safe(cached.getStudentId()));
                        editName.setText(safe(cached.getStudentName()));
                    } else {
                        // 캐시에 없으면 스피너 라벨로 카드 제목 우선 표시
                        updateCardTitle(sel.label);
                        editId.setText(sel.id); // 최소한 ID는 표시
                    }

                    // ★ 항상 상세 API로 원본 전체 필드 재로딩(응답 시 카드 제목도 최종 동기화)
                    loadStudentDetailAsync(sel.id);

                    // 편집 가능(학부모), 저장 가능
                    setFormEnabled(true);
                    editId.setEnabled(false); // 학생 ID는 변경 금지 권장
                    setSaveEnabled(true);
                    showStudentOnlyFields(true);
                }
                updateSaveButtonState();

            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 초기 바인딩은 '나(학부모)' + 카드 제목도 일치
        bindParentFromPrefs();
        updateCardTitle(parentDisplayName);
    }

    /** 자녀 목록 로딩: ParentApi.getChildrenByParentId(parentId) 사용 */
    private void loadChildrenAsync(String parentId) {
        try {
            ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
            Call<List<Student>> call = api.getChildrenByParentId(parentId); // /api/parents/{parentId}/children
            call.enqueue(new Callback<List<Student>>() {
                @Override
                public void onResponse(Call<List<Student>> call, Response<List<Student>> resp) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        Log.w("MyPage", "자녀 목록 응답 실패 code=" + resp.code());
                        return;
                    }
                    for (Student s : resp.body()) {
                        if (s == null) continue;
                        String sid = safe(s.getStudentId());
                        String sname = safe(s.getStudentName());
                        if (sid.isEmpty() || sname.isEmpty()) continue;
                        studentCache.put(sid, s);
                        targets.add(new ProfileItem(ProfileItem.Type.STUDENT, sid, sname));
                    }
                    // 어댑터 갱신
                    List<String> labels = new ArrayList<>();
                    for (ProfileItem it : targets) labels.add(it.label);
                    spinnerAdapter.clear();
                    spinnerAdapter.addAll(labels);
                    spinnerAdapter.notifyDataSetChanged();
                }
                @Override
                public void onFailure(Call<List<Student>> call, Throwable t) {
                    Log.w("MyPage", "자녀 목록 로딩 실패: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e("MyPage", "자녀 목록 로딩 예외: " + e.getMessage(), e);
        }
    }

    /** 학생 상세 개별 로딩(항상 서버에서 원본 재로딩) */
    private void loadStudentDetailAsync(String studentId) {
        try {
            StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
            Call<Student> call = api.getStudentById(studentId);
            call.enqueue(new Callback<Student>() {
                @Override
                public void onResponse(Call<Student> call, Response<Student> resp) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        Log.w("MyPage", "학생 상세 응답 실패 code=" + resp.code());
                        Toast.makeText(MyPageActivity.this, "학생 상세를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Student s = resp.body();
                    if (s.getStudentId() != null) {
                        studentCache.put(s.getStudentId(), s);
                    }
                    // 현재 선택이 이 학생일 때만 카드 제목/폼 갱신(레이스 방지)
                    if (currentSelectionType == ProfileItem.Type.STUDENT &&
                            currentStudentId != null &&
                            currentStudentId.equals(s.getStudentId())) {
                        updateCardTitle(safe(s.getStudentName())); // ★ 카드 제목 최종 동기화
                        bindStudentToForm(s);
                    }
                }
                @Override
                public void onFailure(Call<Student> call, Throwable t) {
                    Log.w("MyPage", "학생 상세 로딩 실패: " + t.getMessage());
                    Toast.makeText(MyPageActivity.this, "네트워크 오류로 학생 상세를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("MyPage", "학생 상세 로딩 예외: " + e.getMessage(), e);
            Toast.makeText(this, "예외가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String safe(String v) { return v == null ? "" : v; }

    private void setupSaveButton() {
        if (btnSave == null) return;

        btnSave.setOnClickListener(v -> {
            // 학생 로그인 자체는 방어
            if ("student".equals(role)) {
                Toast.makeText(this, "학생 계정으로는 수정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 공통 입력값
            String id = safe(editId.getText().toString());
            String name = safe(editName.getText().toString());
            String phone = safe(editPhone.getText().toString());
            String address = safe(editAddress.getText().toString());
            String school = safe(editSchool.getText().toString());
            String gender = safe(editGender.getText().toString());

            // ★ 콜백 안에서 사용할 final 복사본
            final String fId = id;
            final String fName = name;
            final String fPhone = phone;
            final String fAddress = address;
            final String fSchool = school;
            final String fGender = gender;

            // ★ grade를 임시 변수에 담고 final에 단 한 번만 대입
            int tmpGrade;
            try {
                tmpGrade = Integer.parseInt(safe(editGrade.getText().toString()).trim());
            } catch (Exception e) {
                tmpGrade = 0;
            }
            final int fGradeVal = tmpGrade;

            // 간단 유효성
            if (fName.isEmpty()) { Toast.makeText(this, "이름을 입력해 주세요.", Toast.LENGTH_SHORT).show(); return; }

            if ("parent".equals(role)) {
                if (currentSelectionType == ProfileItem.Type.SELF) {
                    // 학부모 본인 수정
                    ParentUpdateRequest parent = new ParentUpdateRequest(fId, fName, fPhone);
                    ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
                    api.updateParent(fId, parent).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            boolean ok = response.isSuccessful();
                            showToast(ok, "학부모");
                            if (ok) {
                                // 서버 성공 시에만 로컬 동기화
                                SharedPreferences pref = getSharedPreferences("login_prefs", MODE_PRIVATE);
                                pref.edit()
                                        .putString("username", fId)
                                        .putString("name", fName)
                                        .putString("phone", fPhone)
                                        .apply();
                                loadUserInfoAndSetTitles();
                                updateCardTitle(parentDisplayName); // 카드 제목 재동기화
                            }
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            showToast(false, "학부모");
                        }
                    });
                } else if (currentSelectionType == ProfileItem.Type.STUDENT && currentStudentId != null) {
                    // ★ 학부모가 선택한 학생 정보 수정
                    StudentUpdateRequest req = new StudentUpdateRequest();
                    // 아래 필드명은 프로젝트 DTO에 맞춰 조정 필요
                    req.setStudentId(currentStudentId);
                    req.setStudentName(fName);
                    req.setStudentPhoneNumber(fPhone);
                    req.setStudentAddress(fAddress);
                    req.setSchool(fSchool);
                    req.setGrade(fGradeVal);
                    req.setGender(fGender);

                    StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
                    api.updateStudent(currentStudentId, req).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            boolean ok = response.isSuccessful();
                            showToast(ok, "학생");
                            if (ok) {
                                // 캐시 값도 갱신
                                Student cached = studentCache.get(currentStudentId);
                                if (cached != null) {
                                    cached.setStudentName(fName);
                                    cached.setStudentPhoneNumber(fPhone);
                                    cached.setStudentAddress(fAddress);
                                    cached.setSchool(fSchool);
                                    cached.setGrade(fGradeVal);
                                    cached.setGender(fGender);
                                }
                                // 카드 제목은 현재 선택된 학생 이름으로 유지
                                updateCardTitle(fName);
                            }
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            showToast(false, "학생");
                        }
                    });
                } else {
                    Toast.makeText(this, "수정할 대상을 선택해 주세요.", Toast.LENGTH_SHORT).show();
                }
            } else if ("teacher".equals(role)) {
                Toast.makeText(this, "교사 정보 수정은 준비 중입니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "현재 역할에서는 수정할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        updateSaveButtonState();
    }

    private void updateSaveButtonState() {
        if (btnSave == null) return;
        // 학부모: 본인/학생 모두 저장 가능
        // 학생: 저장 불가
        // 교사/기타: 본인만 가능(필요 시 정책 조정)
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

    /** 학생 로그인: 모든 필드를 "ID처럼" 읽기전용으로 만들고 저장 버튼 비활성화/숨김 (영구 잠금용) */
    private void lockStudentFields() {
        makeReadOnly(editId);
        makeReadOnly(editName);
        makeReadOnly(editPhone);
        makeReadOnly(editAddress);
        makeReadOnly(editSchool);
        makeReadOnly(editGrade);
        makeReadOnly(editGender);

        tintGray(editId);
        tintGray(editName);
        tintGray(editPhone);
        tintGray(editAddress);
        tintGray(editSchool);
        tintGray(editGrade);
        tintGray(editGender);

        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setVisibility(View.GONE);
        }
    }

    /** 외형은 유지하면서 입력만 차단(복사 허용) */
    private void makeReadOnly(EditText et) {
        if (et == null) return;
        et.setFocusable(false);
        et.setFocusableInTouchMode(false);
        et.setClickable(false);
        et.setLongClickable(false);
        et.setCursorVisible(false);
        et.setTextIsSelectable(true); // 복사 허용
        et.setKeyListener(null);      // 핵심: 키 입력 차단
    }

    /** 텍스트만 회색으로 */
    private void tintGray(EditText et) {
        if (et == null) return;
        et.setTextColor(Color.parseColor("#6B7280"));   // 중간 회색
        et.setHintTextColor(Color.parseColor("#9AA0A6")); // 힌트는 더 옅게
    }

    /** SharedPreferences 값으로 '학부모' 폼 바인딩 */
    private void bindParentFromPrefs() {
        SharedPreferences pref = getSharedPreferences("login_prefs", MODE_PRIVATE);
        editName.setText(pref.getString("name", ""));
        editId.setText(pref.getString("username", ""));
        editPhone.setText(pref.getString("phone", ""));

        // 학생 전용 필드는 감춤/초기화
        editAddress.setText("");
        editSchool.setText("");
        editGrade.setText("");
        editGender.setText("");
    }

    /** Student 객체를 공용 폼에 바인딩(표시용) */
    private void bindStudentToForm(Student s) {
        if (s == null) return;
        editName.setText(safe(s.getStudentName()));
        editId.setText(safe(s.getStudentId()));
        editPhone.setText(safe(s.getStudentPhoneNumber()));
        editAddress.setText(safe(s.getStudentAddress()));
        editSchool.setText(safe(s.getSchool()));

        // grade: 0이면 빈칸
        try {
            int g = s.getGrade();
            editGrade.setText(g == 0 ? "" : String.valueOf(g));
        } catch (Exception e) {
            editGrade.setText("");
        }

        editGender.setText(safe(s.getGender()));
    }

    /** 폼 입력 활성/비활성(되돌릴 수 있는 임시 토글) */
    private void setFormEnabled(boolean enabled) {
        EditText[] arr = new EditText[]{ editName, editId, editPhone, editAddress, editSchool, editGrade, editGender };
        for (EditText et : arr) { if (et != null) et.setEnabled(enabled); }
    }

    /** 저장 버튼 활성/표시 토글 */
    private void setSaveEnabled(boolean enabled) {
        if (btnSave == null) return;
        btnSave.setEnabled(enabled);
        btnSave.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    /** EditText의 부모(행 컨테이너)까지 함께 토글 */
    private void setRowVisibility(EditText et, int visibility) {
        if (et == null) return;

        et.setVisibility(visibility);

        View p1 = (View) et.getParent();
        if (p1 != null) {
            p1.setVisibility(visibility);
            View p2 = (View) p1.getParent();
            if (p2 != null) {
                p2.setVisibility(visibility);
            }
        }
    }

    /** 학생 전용 필드 표시 여부(주소/학교/학년/성별) — 컨테이너까지 함께 토글 + 자기 화면 복귀 시 값 초기화 */
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
