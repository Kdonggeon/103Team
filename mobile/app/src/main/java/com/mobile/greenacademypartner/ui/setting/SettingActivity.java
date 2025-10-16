package com.mobile.greenacademypartner.ui.setting;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.ui.attendance.AttendanceActivity;
import com.mobile.greenacademypartner.ui.login.LoginActivity;
import com.mobile.greenacademypartner.ui.main.MainActivity;
import com.mobile.greenacademypartner.ui.mypage.MyPageActivity;
import com.mobile.greenacademypartner.ui.notice.NoticeActivity;
import com.mobile.greenacademypartner.ui.timetable.QRScannerActivity;
import com.mobile.greenacademypartner.ui.timetable.StudentTimetableActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private GridLayout colorGrid;
    private Button btnLogout;

    private LinearLayout boxNotifications;
    private SwitchCompat swInApp;

    private LinearLayout boxFonts;
    private RadioButton rbSystem, rbNoto;

    private ColorStateList NEUTRAL_THUMB;
    private ColorStateList NEUTRAL_TRACK;
    private ColorStateList NEUTRAL_RADIO;

    // ✅ 네비게이션 토글 버튼
    private ImageButton btnHideNav, btnShowNav;
    private BottomNavigationView bottomNavigation;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        toolbar = findViewById(R.id.toolbar_setting);
        colorGrid = findViewById(R.id.color_grid);
        btnLogout = findViewById(R.id.btn_logout);
        boxNotifications = findViewById(R.id.box_notifications);
        swInApp = findViewById(R.id.switch_inapp_notifications);

        // ✅ 토글 버튼 & 네비게이션 연결
        bottomNavigation = findViewById(R.id.bottom_navigation);
        btnHideNav = findViewById(R.id.btn_hide_nav);
        btnShowNav = findViewById(R.id.btn_show_nav);

        // 🔽 네비게이션 숨기기
        btnHideNav.setOnClickListener(v -> {
            bottomNavigation.setVisibility(View.GONE);
            btnHideNav.setVisibility(View.GONE);
            btnShowNav.setVisibility(View.VISIBLE);
        });

        // 🔼 네비게이션 보이기
        btnShowNav.setOnClickListener(v -> {
            bottomNavigation.setVisibility(View.VISIBLE);
            btnShowNav.setVisibility(View.GONE);
            btnHideNav.setVisibility(View.VISIBLE);
        });

        // ===== 초기화 =====
        View content = findViewById(android.R.id.content);
        if (content != null) content.setBackgroundColor(Color.WHITE);
        buildNeutralPalettes();

        swInApp.setSplitTrack(false);
        swInApp.setShowText(false);
        swInApp.setThumbTintList(null);
        swInApp.setTrackTintList(null);
        ViewCompat.setBackgroundTintList(swInApp, null);
        swInApp.setBackground(null);
        clearSwitchBackdrop();
        disableMaterialThemeColors(swInApp);

        ColorStateList trackXml = ContextCompat.getColorStateList(this, R.color.switch_track_neutral);
        ColorStateList thumbXml = ContextCompat.getColorStateList(this, R.color.switch_thumb_neutral);
        if (trackXml != null) swInApp.setTrackTintList(trackXml);
        if (thumbXml != null) swInApp.setThumbTintList(thumbXml);

        ToolbarColorUtil.applyToolbarColor(this, toolbar);
        setSupportActionBar(toolbar);
        toolbar.bringToFront();
        ViewCompat.setElevation(toolbar, dp(6));

        insertThemeTitleAboveGrid();
        decorateColorGrid();
        decorateNotificationCard();

        if (findViewById(R.id.card_font_settings) != null) {
            bindFontControlsFromXml();
        } else {
            insertFontSectionBelowNotifications();
        }

        setupAddChildCardFromXml();
        setupColorSelection();

        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences login = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String currentUserId = login.getString("username", "");
        String notifKey = "notifications_enabled_" + currentUserId;
        boolean enabled = settings.getBoolean(notifKey, true);
        swInApp.setChecked(enabled);

        applyNeutralTints();
        clearSwitchBackdrop();

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
        });

        btnLogout.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            Intent intent = new Intent(SettingActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        ThemeColorUtil.applyThemeColor(this, toolbar);

        // ✅ 하단 네비게이션 이동
        bottomNavigation.setOnItemSelectedListener(item -> {
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
                overridePendingTransition(0,0);
                return true;
            } else if (id == R.id.nav_my) {
                startActivity(new Intent(this, MyPageActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        disableMaterialThemeColors(swInApp);
        applyNeutralTints();
        clearSwitchBackdrop();
    }

    /** ===== 유틸 메소드 ===== */

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
                glp.setMargins(dp(16), dp(20), dp(16), dp(16));
                colorGrid.setLayoutParams(glp);
            }
        }
    }

    private void bindFontControlsFromXml() {
        RadioGroup group = findViewById(R.id.rg_font_choice);
        rbSystem = findViewById(R.id.rb_font_system);
        rbNoto   = findViewById(R.id.rb_font_noto);
        View btnApply = findViewById(R.id.btn_font_apply);

        if (group == null || rbSystem == null || rbNoto == null || btnApply == null) return;

        SharedPreferences sp = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String cur = sp.getString("app_font", "System");
        if ("NotoSansKR".equals(cur)) rbNoto.setChecked(true); else rbSystem.setChecked(true);

        btnApply.setOnClickListener(v -> {
            String sel = rbNoto.isChecked() ? "NotoSansKR" : "System";
            sp.edit().putString("app_font", sel).apply();
            recreate();
        });
    }

    private void insertFontSectionBelowNotifications() {
        if (boxNotifications == null) return;
        if (findViewById(R.id.card_font_settings) != null) return;

        boxFonts = new LinearLayout(this);
        boxFonts.setOrientation(LinearLayout.VERTICAL);
        decorateCardLike(boxFonts);

        TextView title = new TextView(this);
        title.setText("폰트 설정");
        title.setTextSize(16f);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setTextColor(Color.BLACK);
        title.setPadding(0, 0, 0, dp(8));
        boxFonts.addView(title);

        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);

        rbSystem = new RadioButton(this);
        rbSystem.setText("기본 폰트(시스템)");
        rbSystem.setBackgroundColor(Color.TRANSPARENT);
        rbNoto = new RadioButton(this);
        rbNoto.setText("Noto Sans KR");
        rbNoto.setBackgroundColor(Color.TRANSPARENT);

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

        RelativeLayout pr = (RelativeLayout) boxNotifications.getParent();
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        lp.addRule(RelativeLayout.BELOW, boxNotifications.getId());
        lp.setMargins(dp(16), dp(12), dp(16), dp(16));
        pr.addView(boxFonts, lp);
    }

    private void setupAddChildCardFromXml() {
        View addCard = findViewById(R.id.card_add_child_settings);
        View addBtn  = findViewById(R.id.btn_add_child_settings);
        if (addCard == null || addBtn == null) return;

        decorateCardLike(addCard);

        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", "");

        if (!"parent".equalsIgnoreCase(role)) {
            addCard.setVisibility(View.GONE);
            return;
        }
        addCard.setVisibility(View.VISIBLE);

        addBtn.setOnClickListener(v -> launchAddChildActivityDirect());
    }

    private void launchAddChildActivityDirect() {
        String[] candidates = new String[] {
                "com.mobile.greenacademypartner.ui.timetable.AddChildActivity",
                "com.mobile.greenacademypartner.ui.parents.AddChildActivity",
                "com.mobile.greenacademypartner.ui.parent.AddChildActivity",
                "com.mobile.greenacademypartner.ui.mypage.AddChildActivity",
                "com.mobile.greenacademypartner.ui.setting.AddChildActivity"
        };
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String parentId = firstNonEmpty(
                prefs.getString("parentId", null),
                prefs.getString("userId", null),
                prefs.getString("username", null)
        );

        for (String fqcn : candidates) {
            try {
                Class<?> clz = Class.forName(fqcn);
                Intent intent = new Intent(this, (Class<? extends Activity>) clz);
                if (parentId != null) intent.putExtra("parentId", parentId);
                startActivity(intent);
                return;
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                Log.e("Settings", "자녀 추가 화면 실행 실패: " + fqcn, t);
            }
        }
        Toast.makeText(this, "자녀 추가 화면 클래스를 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
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
                ThemeColorUtil.applyThemeColor(SettingActivity.this, toolbar);
                applyNeutralTints();
                clearSwitchBackdrop();
            });

            container.addView(chip);
            colorGrid.addView(container);
        }
    }

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

    private void buildNeutralPalettes() {
        NEUTRAL_THUMB = ColorStateList.valueOf(Color.parseColor("#FAFAFA"));
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked},
        };
        int[] colors = new int[]{
                Color.parseColor("#C9CDD2"),
                Color.parseColor("#DDE1E6")
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
            rbSystem.setBackgroundColor(Color.TRANSPARENT);
        }
        if (rbNoto != null) {
            CompoundButtonCompat.setButtonTintList(rbNoto, NEUTRAL_RADIO);
            rbNoto.setTextColor(Color.BLACK);
            rbNoto.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void disableMaterialThemeColors(View v) {
        if (v == null) return;
        try {
            java.lang.reflect.Method m = v.getClass().getMethod("setUseMaterialThemeColors", boolean.class);
            m.invoke(v, false);
        } catch (Throwable t) {
            Log.w("Switch", "disableMaterialThemeColors failed: " + t.getMessage());
        }
    }

    private void clearSwitchBackdrop() {
        if (swInApp == null) return;
        try {
            swInApp.setBackground(null);
            ViewCompat.setBackgroundTintList(swInApp, null);
            if (Build.VERSION.SDK_INT >= 23) swInApp.setForeground(null);
        } catch (Throwable ignored) {}
    }

    private void updateServerToken(String role, String token, SharedPreferences login) {
        if (role == null) return;

        String idStudent = login.getString("studentId", null);
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
