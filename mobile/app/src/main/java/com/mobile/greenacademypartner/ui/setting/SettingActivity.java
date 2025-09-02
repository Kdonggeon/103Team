package com.mobile.greenacademypartner.ui.setting;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;
import com.mobile.greenacademypartner.menu.ToolbarColorUtil;
import com.mobile.greenacademypartner.ui.login.LoginActivity;

public class SettingActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout navContainer;
    private TextView mainContentText;
    private GridLayout colorGrid;
    private Button btnLogout;

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

        // 2) 배경 흰색
        // (루트/콘텐츠를 흰색으로 – XML 수정 없이 적용)
        drawerLayout.setBackgroundColor(Color.WHITE);
        View content = findViewById(android.R.id.content);
        if (content != null) content.setBackgroundColor(Color.WHITE);

        // 3) 툴바 색/설정 및 항상 최상단으로 (박스가 덮는 현상 방지)
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

        // 8) 색상 칩 채우기
        setupColorSelection();

        // 9) 로그아웃
        btnLogout.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            Intent intent = new Intent(SettingActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 10) 설정 메뉴 선택 표시 및 테마 적용
        View settingView = navContainer.getChildAt(defaultIndex);
        if (settingView != null) settingView.performClick();
        ThemeColorUtil.applyThemeColor(this, toolbar);
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

            // 제목: 툴바 아래
            RelativeLayout.LayoutParams tlp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            tlp.addRule(RelativeLayout.BELOW, R.id.toolbar_setting);
            tlp.setMargins(dp(16), dp(16), dp(16), 0);
            pr.addView(title, tlp);

            // 그리드: 제목 아래로 재배치 + 여백
            ViewGroup.LayoutParams lp0 = colorGrid.getLayoutParams();
            if (lp0 instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams glp = (RelativeLayout.LayoutParams) lp0;
                glp.addRule(RelativeLayout.BELOW, titleId);
                glp.setMargins(dp(16), dp(8), dp(16), dp(16));
                colorGrid.setLayoutParams(glp);
            }
        } else if (parent instanceof LinearLayout) {
            // LinearLayout이면 그리드 앞에 제목을 끼워 넣기
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
            pr.addView(title, idx); // colorGrid 바로 위에 삽입

            // 그리드 여백
            ViewGroup.LayoutParams lp0 = colorGrid.getLayoutParams();
            if (lp0 instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams glp = (LinearLayout.LayoutParams) lp0;
                glp.setMargins(dp(16), dp(8), dp(16), dp(16));
                colorGrid.setLayoutParams(glp);
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

        for (int i = 0; i < colorValues.length; i++) {
            final int color = colorValues[i];

            // 제목+칩 컨테이너(수직)
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            glp.setMargins(dp(8), dp(8), dp(8), dp(8));
            container.setLayoutParams(glp);

            // 제목
            TextView titleView = new TextView(this);
            titleView.setTextSize(14f);
            titleView.setTextColor(Color.BLACK);
            titleView.setPadding(0, 0, 0, dp(6));

            // 색상 칩(둥근 버튼)
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

    /** 개별 색상 칩 배경: 내부색 + 둥근 모서리 + 얇은 테두리 + 리플 */
    private android.graphics.drawable.Drawable makeChipBackground(int fillColor) {
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
}
