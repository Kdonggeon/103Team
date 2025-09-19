package com.mobile.greenacademypartner.ui.setting;

import android.annotation.SuppressLint;
import android.content.Intent;
import androidx.core.widget.CompoundButtonCompat;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.messaging.FirebaseMessaging;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.api.TeacherApi;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.ui.login.LoginActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private TextView mainContentText;
    private GridLayout colorGrid;
    private Button btnLogout;

    private LinearLayout boxNotifications;
    private SwitchCompat swInApp;

    // 폰트 설정 섹션(동적)
    private LinearLayout boxFonts;
    private RadioButton rbSystem, rbNoto;

    // 중립(고정) 틴트 팔레트
    private ColorStateList NEUTRAL_THUMB;   // 스위치 손잡이
    private ColorStateList NEUTRAL_TRACK;   // 스위치 트랙
    private ColorStateList NEUTRAL_RADIO;   // 라디오 버튼

    int defaultIndex = 5;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // 1) 뷰 초기화
        drawerLayout = findViewById(R.id.drawer_layout_setting);
        toolbar = findViewById(R.id.toolbar_setting);
        navContainer = findViewById(R.id.nav_container_setting);
        colorGrid = findViewById(R.id.color_grid);
        mainContentText = findViewById(R.id.main_content_text);
        btnLogout = findViewById(R.id.btn_logout);

        boxNotifications = findViewById(R.id.box_notifications);
        swInApp = findViewById(R.id.switch_inapp_notifications);

        // 2) 배경 흰색 + 중립 팔레트 구성
        drawerLayout.setBackgroundColor(Color.WHITE);
        View content = findViewById(android.R.id.content);
        if (content != null) content.setBackgroundColor(Color.WHITE);
        buildNeutralPalettes();

        swInApp.setSplitTrack(false);
        swInApp.setShowText(false);
        swInApp.setThumbTintList(null);
        swInApp.setTrackTintList(null);
        ViewCompat.setBackgroundTintList(swInApp, null);
        swInApp.setBackground(null);
        clearSwitchBackdrop(); // 최초 배경 제거
        disableMaterialThemeColors(swInApp);

        // XML tint가 있다면 보강 적용(없어도 무방)
        ColorStateList trackXml = ContextCompat.getColorStateList(this, R.color.switch_track_neutral);
        ColorStateList thumbXml = ContextCompat.getColorStateList(this, R.color.switch_thumb_neutral);
        if (trackXml != null) swInApp.setTrackTintList(trackXml);
        if (thumbXml != null) swInApp.setThumbTintList(thumbXml);

        // 3) 툴바 색/설정 및 항상 최상단
        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);
        toolbar.bringToFront();
        ViewCompat.setElevation(toolbar, dp(6));

        // 4) 드로어 토글
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 5) 메뉴
        NavigationMenuHelper.setupMenu(this, navContainer, drawerLayout, mainContentText, defaultIndex);

        // 6) “테마 색상 변경” 제목을 colorGrid 위에 동적 추가
        insertThemeTitleAboveGrid();

        // 7) 색상 박스(그룹) 데코/패딩
        decorateColorGrid();

        // 7-1) 인앱 알림 카드 동일 스타일
        decorateNotificationCard();

        // 7-2) 폰트 설정 섹션 삽입
        insertFontSectionBelowNotifications();

        // 8) 색상 칩 채우기
        setupColorSelection();

        // 9) 인앱 알림 스위치 상태
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences login = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String currentUserId = login.getString("username", "");
        String notifKey = "notifications_enabled_" + currentUserId;
        boolean enabled = settings.getBoolean(notifKey, true);
        swInApp.setChecked(enabled);

        // 9-1) 중립 틴트 최초/지연 적용 + 배경 제거
        applyNeutralTints();
        clearSwitchBackdrop();
        swInApp.post(() -> {
            applyNeutralTints();
            clearSwitchBackdrop();
        });

        swInApp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.edit().putBoolean(notifKey, isChecked).apply();
            String role = login.getString("role", null);

            if (!isChecked) {
                FirebaseMessaging.getInstance().deleteToken()
                        .addOnCompleteListener(task -> updateServerToken(role, "", login));
            } else {
                FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String token = task.getResult();
                                updateServerToken(role, token, login);
                            } else {
                                Log.w("Settings", "토큰 재발급 실패", task.getException());
                            }
                        });
            }

            applyNeutralTints();
            clearSwitchBackdrop();
            swInApp.post(() -> {
                applyNeutralTints();
                clearSwitchBackdrop();
            });
        });

        // 10) 로그아웃
        btnLogout.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            Intent intent = new Intent(SettingActivity.this, LoginActivity.class); // 수정됨
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 11) 설정 메뉴 선택 표시 및 테마 적용 → 즉시 중립 틴트/배경 재적용
        View settingView = navContainer.getChildAt(defaultIndex);
        if (settingView != null) settingView.performClick();
        com.mobile.greenacademypartner.ui.setting.ThemeColorUtil.applyThemeColor(this, toolbar);
        applyNeutralTints();
        clearSwitchBackdrop();
        swInApp.post(() -> {
            applyNeutralTints();
            clearSwitchBackdrop();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        disableMaterialThemeColors(swInApp);
        applyNeutralTints();
        clearSwitchBackdrop();
        swInApp.post(() -> {
            applyNeutralTints();
            clearSwitchBackdrop();
        });
    }

    /** colorGrid 위에 “테마 색상 변경” 제목을 추가(새 ID 없이 런타임 배치) */
    private void insertThemeTitleAboveGrid() {
        if (colorGrid == null) return;
        ViewParent parent = colorGrid.getParent();
        if (parent instanceof RelativeLayout) {
            RelativeLayout pr = (RelativeLayout) parent;

            TextView title = new TextView(this);
            title.setText("테마 색상 변경");
            title.setTextSize(18f);
            title.setTypeface(title.getTypeface(), Typeface.BOLD);
            title.setTextColor(Color.BLACK);

            int titleId = View.generateViewId();
            title.setId(titleId);

            RelativeLayout.LayoutParams tlp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            tlp.addRule(RelativeLayout.BELOW, R.id.toolbar_setting);
            tlp.setMargins(dp(16), dp(16), dp(16), 0);
            pr.addView(title, tlp);

            ViewGroup.LayoutParams lp0 = colorGrid.getLayoutParams();
            if (lp0 instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams glp = (RelativeLayout.LayoutParams) lp0;
                glp.addRule(RelativeLayout.BELOW, titleId);
                glp.setMargins(dp(16), dp(8), dp(16), dp(16));
                colorGrid.setLayoutParams(glp);
            }
        } else if (parent instanceof LinearLayout) {
            LinearLayout pr = (LinearLayout) parent;
            int idx = pr.indexOfChild(colorGrid);

            TextView title = new TextView(this);
            title.setText("테마 색상 변경");
            title.setTextSize(18f);
            title.setTypeface(title.getTypeface(), Typeface.BOLD);
            title.setTextColor(Color.BLACK);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            tlp.setMargins(dp(16), dp(16), dp(16), 0);
            pr.addView(title, idx);

            ViewGroup.LayoutParams lp0 = colorGrid.getLayoutParams();
            if (lp0 instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams glp = (LinearLayout.LayoutParams) lp0;
                glp.setMargins(dp(16), dp(8), dp(16), dp(16));
                colorGrid.setLayoutParams(glp);
            }
        }
    }

    /** 알림 카드 바로 아래에 “폰트 설정” 카드(동적) 추가: 기본↔Noto 토글 */
    private void insertFontSectionBelowNotifications() {
        if (boxNotifications == null) return;

        boxFonts = new LinearLayout(this);
        boxFonts.setOrientation(LinearLayout.VERTICAL);
        decorateCardLike(boxFonts);

        TextView title = new TextView(this);
        title.setText("폰트 설정");
        title.setTextSize(16f);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setTextColor(Color.BLACK);
        title.setPadding(0, 0, 0, dp(8));
        if (boxFonts instanceof ViewGroup) ((ViewGroup) boxFonts).addView(title);

        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);

        rbSystem = new RadioButton(this);
        rbSystem.setText("기본 폰트(시스템)");

        rbNoto = new RadioButton(this);
        rbNoto.setText("Noto Sans KR");

        group.addView(rbSystem);
        group.addView(rbNoto);
        boxFonts.addView(group);

        Button apply = new Button(this);
        apply.setText("적용");
        apply.setAllCaps(false);
        boxFonts.addView(apply);

        SharedPreferences sp = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String cur = sp.getString("app_font", "System");
        if ("NotoSansKR".equals(cur)) rbNoto.setChecked(true); else rbSystem.setChecked(true);

        apply.setOnClickListener(v -> {
            String sel = rbNoto.isChecked() ? "NotoSansKR" : "System";
            sp.edit().putString("app_font", sel).apply();
            recreate();
        });

        ViewParent parent = boxNotifications.getParent();
        if (parent instanceof RelativeLayout) {
            RelativeLayout pr = (RelativeLayout) parent;
            int fontId = View.generateViewId();
            boxFonts.setId(fontId);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            lp.addRule(RelativeLayout.BELOW, boxNotifications.getId());
            lp.setMargins(dp(16), dp(12), dp(16), dp(16));
            pr.addView(boxFonts, lp);
        } else if (parent instanceof LinearLayout) {
            LinearLayout pr = (LinearLayout) parent;
            int idx = pr.indexOfChild(boxNotifications);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(dp(16), dp(12), dp(16), dp(16));
            pr.addView(boxFonts, idx + 1);
            boxFonts.setLayoutParams(lp);
        } else {
            ViewGroup root = findViewById(android.R.id.content);
            if (root instanceof ViewGroup && ((ViewGroup) root).getChildCount() > 0) {
                ViewGroup contentRoot = (ViewGroup) ((ViewGroup) root).getChildAt(0);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(dp(16), dp(12), dp(16), dp(16));
                contentRoot.addView(boxFonts, lp);
            }
        }
    }

    private void setupColorSelection() {
        int[] colorValues = {
                getColor(R.color.green),
                Color.rgb(168, 230, 207),
                Color.rgb(246, 234, 194),
                Color.rgb(231, 201, 215),
                Color.rgb(255, 209, 193),
                Color.rgb(195, 205, 230),
                Color.rgb(248, 187, 208),
                Color.rgb(174, 238, 238)
        };

        colorGrid.setColumnCount(4);
        colorGrid.setUseDefaultMargins(true);

        for (int color : colorValues) {
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            glp.setMargins(dp(8), dp(8), dp(8), dp(8));
            container.setLayoutParams(glp);

            TextView titleView = new TextView(this);
            titleView.setTextSize(14f);
            titleView.setTextColor(Color.BLACK);
            titleView.setPadding(0, 0, dp(0), dp(6));

            View chip = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(72), dp(44));
            chip.setLayoutParams(lp);
            chip.setClickable(true);
            chip.setBackground(makeChipBackground(color));

            chip.setOnClickListener(v -> {
                toolbar.setBackgroundColor(color);
                getSharedPreferences("settings", MODE_PRIVATE)
                        .edit()
                        .putInt("theme_color", color)
                        .apply();
                com.mobile.greenacademypartner.ui.setting.ThemeColorUtil.applyThemeColor(SettingActivity.this, toolbar);

                applyNeutralTints();
                clearSwitchBackdrop();
                swInApp.post(() -> {
                    applyNeutralTints();
                    clearSwitchBackdrop();
                });
            });

            container.addView(titleView);
            container.addView(chip);
            colorGrid.addView(container);
        }
    }

    /** 색상 버튼 묶음(그리드) - 둥근 박스 + 패딩 */
    private void decorateColorGrid() {
        if (colorGrid == null) return;
        colorGrid.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F6F8FA"));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0x1A000000);
        colorGrid.setBackground(bg);
    }

    private void decorateNotificationCard() {
        if (boxNotifications == null) return;
        decorateCardLike(boxNotifications);
    }

    private void decorateCardLike(View target) {
        if (target == null) return;
        if (target instanceof ViewGroup) {
            ((ViewGroup) target).setPadding(dp(16), dp(16), dp(16), dp(16));
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F6F8FA"));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0x1A000000);
        if (Build.VERSION.SDK_INT >= 21) {
            ColorStateList ripple = ColorStateList.valueOf(0x11000000);
            target.setBackground(new RippleDrawable(ripple, bg, null));
        } else {
            target.setBackground(bg);
        }
    }

    private Drawable makeChipBackground(int fillColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fillColor);
        shape.setCornerRadius(dp(16));
        shape.setStroke(dp(1), 0x33000000);

        if (Build.VERSION.SDK_INT >= 21) {
            ColorStateList ripple = ColorStateList.valueOf(0x22000000);
            return new RippleDrawable(ripple, shape, null);
        }
        return shape;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ===== 중립(고정) 팔레트 구성 & 적용 =====
    private void buildNeutralPalettes() {
        NEUTRAL_THUMB = ColorStateList.valueOf(Color.parseColor("#FAFAFA")); // 손잡이
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked},
        };
        int[] colors = new int[]{
                Color.parseColor("#C9CDD2"),  // 체크 트랙
                Color.parseColor("#DDE1E6")   // 미체크 트랙
        };
        NEUTRAL_TRACK = new ColorStateList(states, colors);
        NEUTRAL_RADIO = ColorStateList.valueOf(Color.parseColor("#C9CDD2"));
    }

    private void applyNeutralTints() {
        if (swInApp != null) {
            try {
                disableMaterialThemeColors(swInApp);

                Drawable track = swInApp.getTrackDrawable();
                if (track != null) {
                    track = DrawableCompat.wrap(track.mutate());
                    DrawableCompat.setTintList(track, NEUTRAL_TRACK);
                    swInApp.setTrackDrawable(track);
                }
                Drawable thumb = swInApp.getThumbDrawable();
                if (thumb != null) {
                    thumb = DrawableCompat.wrap(thumb.mutate());
                    DrawableCompat.setTintList(thumb, NEUTRAL_THUMB);
                    swInApp.setThumbDrawable(thumb);
                }
                swInApp.setTrackTintList(NEUTRAL_TRACK);
                swInApp.setThumbTintList(NEUTRAL_THUMB);
            } catch (Throwable ignored) {}
        }

        if (rbSystem != null) {
            CompoundButtonCompat.setButtonTintList(rbSystem, NEUTRAL_RADIO);
            rbSystem.setTextColor(Color.BLACK);
            if (Build.VERSION.SDK_INT >= 21) {
                ColorStateList ripple = ColorStateList.valueOf(0x11000000);
                rbSystem.setBackground(new RippleDrawable(ripple, null, null));
            } else {
                rbSystem.setBackground(null);
            }
        }
        if (rbNoto != null) {
            CompoundButtonCompat.setButtonTintList(rbNoto, NEUTRAL_RADIO);
            rbNoto.setTextColor(Color.BLACK);
            if (Build.VERSION.SDK_INT >= 21) {
                ColorStateList ripple = ColorStateList.valueOf(0x11000000);
                rbNoto.setBackground(new RippleDrawable(ripple, null, null));
            } else {
                rbNoto.setBackground(null);
            }
        }
    }

    private void disableMaterialThemeColors(View v) {
        if (v == null) return;
        try {
            java.lang.reflect.Method m = v.getClass().getMethod("setUseMaterialThemeColors", boolean.class);
            m.invoke(v, false);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
            Log.w("Switch", "disableMaterialThemeColors failed: " + t.getMessage());
        }
    }

    /** 스위치 주변에 씌워진 배경/리플 등을 완전히 제거 */
    private void clearSwitchBackdrop() {
        if (swInApp == null) return;
        try {
            Drawable bg = swInApp.getBackground();
            if (bg instanceof RippleDrawable) {
                ((RippleDrawable) bg).setColor(ColorStateList.valueOf(Color.TRANSPARENT));
            }
            swInApp.setBackground(null);
            ViewCompat.setBackgroundTintList(swInApp, null);
            if (Build.VERSION.SDK_INT >= 23) swInApp.setForeground(null);
        } catch (Throwable ignored) {}
    }

    // ===== FCM 토큰 서버 반영 (ID 폴백 포함) =====
    private void updateServerToken(String role, String token, SharedPreferences login) {
        if (role == null) return;

        String idStudent = login.getString("studentId", null);
        String idTeacher = login.getString("teacherId", null);
        String idParent = login.getString("parentId", null);
        String username = firstNonEmpty(login.getString("userId", null),
                login.getString("username", null));

        if ("student".equalsIgnoreCase(role)) {
            String id = firstNonEmpty(idStudent, username);
            if (id == null) return;
            StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
            api.updateFcmToken(id, token == null ? "" : token).enqueue(new Callback<Void>() {
                public void onResponse(Call<Void> c, Response<Void> r) {
                    Log.d("Settings", "학생 토큰 업데이트");
                }
                public void onFailure(Call<Void> c, Throwable t) {
                    Log.e("Settings", "학생 토큰 실패", t);
                }
            });

        } else if ("parent".equalsIgnoreCase(role)) {
            String id = firstNonEmpty(idParent, username);
            if (id == null) return;
            ParentApi api = RetrofitClient.getClient().create(ParentApi.class);
            api.updateFcmToken(id, token == null ? "" : token).enqueue(new Callback<Void>() {
                public void onResponse(Call<Void> c, Response<Void> r) {
                    Log.d("Settings", "부모 토큰 업데이트");
                }
                public void onFailure(Call<Void> c, Throwable t) {
                    Log.e("Settings", "부모 토큰 실패", t);
                }
            });

        } else { // teacher/director
            String id = (idTeacher != null && !idTeacher.trim().isEmpty()) ? idTeacher.trim() : null;
            if (id == null) {
                Log.e("Settings", "교사/원장 토큰 업데이트 중단: teacherId 없음(폴백 금지)");
                return;
            }
            TeacherApi api = RetrofitClient.getClient().create(TeacherApi.class);
            api.updateFcmToken(id, token == null ? "" : token).enqueue(new Callback<Void>() {
                public void onResponse(Call<Void> c, Response<Void> r) {
                    Log.d("Settings", "교사/원장 토큰 업데이트");
                }
                public void onFailure(Call<Void> c, Throwable t) {
                    Log.e("Settings", "교사/원장 토큰 실패", t);
                }
            });
        }
    }

    private String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) return v;
        }
        return null;
    }
}
