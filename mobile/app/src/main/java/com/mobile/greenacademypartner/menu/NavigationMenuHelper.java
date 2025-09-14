package com.mobile.greenacademypartner.menu;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.ui.main.MainActivity;
import com.mobile.greenacademypartner.ui.attendance.AttendanceActivity;
import com.mobile.greenacademypartner.ui.mypage.MyPageActivity;
import com.mobile.greenacademypartner.ui.notice.NoticeActivity;
import com.mobile.greenacademypartner.ui.qna.QuestionsActivity;
import com.mobile.greenacademypartner.ui.setting.SettingActivity;

public class NavigationMenuHelper {

    // ✅ 역할 구분 (TEACHER/DIRECTOR 남겨서 컴파일 안전, 내부에서는 폴백 처리)
    public enum Role { STUDENT, PARENT, TEACHER, DIRECTOR }

    public static class MenuSpec {
        public final String label;
        public final int iconLight;
        public final int iconDark;
        public final Class<?> targetActivity;
        public final boolean alwaysLaunchNew;

        public MenuSpec(String label, int iconLight, int iconDark, Class<?> targetActivity) {
            this(label, iconLight, iconDark, targetActivity, false);
        }
        public MenuSpec(String label, int iconLight, int iconDark, Class<?> targetActivity, boolean alwaysLaunchNew) {
            this.label = label;
            this.iconLight = iconLight;
            this.iconDark = iconDark;
            this.targetActivity = targetActivity;
            this.alwaysLaunchNew = alwaysLaunchNew;
        }
    }

    private static LinearLayout selectedItem = null;

    // ✅ 기존 API와의 호환을 위한 기본 배열 (학생/학부모 공용 메뉴)
    public static final String[] labels = {
            "My Page", "출석 관리", "시간표", "Q&A", "공지사항", "설정"
    };

    private static final int[] icons = {
            R.drawable.ic_person_light,
            R.drawable.ic_attendance_light,
            R.drawable.ic_timetable_light,
            R.drawable.ic_qa_light,
            R.drawable.ic_notice_light,
            R.drawable.ic_settings_light
    };

    private static final int[] icons_dark = {
            R.drawable.ic_person_dark,
            R.drawable.ic_attendance_dark,
            R.drawable.ic_timetable_dark,
            R.drawable.ic_qa_dark,
            R.drawable.ic_notice_dark,
            R.drawable.ic_settings_dark
    };

    private static final Class<?>[] targetActivities = {
            MyPageActivity.class,
            AttendanceActivity.class,
            MainActivity.class,
            QuestionsActivity.class,
            NoticeActivity.class,
            SettingActivity.class
    };

    // 기존 호출 유지용 (역할 구분 없이 기본 메뉴 세팅)
    public static void setupMenu(Activity activity, LinearLayout navContainer, DrawerLayout drawerLayout, TextView mainContentText, int initialSelectedIndex) {
        MenuSpec[] specs = new MenuSpec[labels.length];
        for (int i = 0; i < labels.length; i++) {
            boolean alwaysNew = (targetActivities[i] == AttendanceActivity.class);
            specs[i] = new MenuSpec(labels[i], icons[i], icons_dark[i], targetActivities[i], alwaysNew);
        }
        setupMenu(activity, navContainer, drawerLayout, mainContentText, initialSelectedIndex, specs);
    }

    // ✅ 역할별 메뉴: TEACHER/DIRECTOR도 기본 메뉴로 폴백 (기능 제거)
    public static MenuSpec[] getMenuForRole(Role role) {
        // STUDENT/PARENT 동일 구성 사용
        MenuSpec[] defaultSpecs = new MenuSpec[labels.length];
        for (int i = 0; i < labels.length; i++) {
            boolean alwaysNew = (targetActivities[i] == AttendanceActivity.class);
            defaultSpecs[i] = new MenuSpec(labels[i], icons[i], icons_dark[i], targetActivities[i], alwaysNew);
        }
        return defaultSpecs;
    }

    // 역할 지정 버전
    public static void setupMenu(Activity activity,
                                 LinearLayout navContainer,
                                 DrawerLayout drawerLayout,
                                 TextView mainContentText,
                                 int initialSelectedIndex,
                                 Role role) {
        setupMenu(activity, navContainer, drawerLayout, mainContentText, initialSelectedIndex, getMenuForRole(role));
    }

    // 공통 렌더러
    public static void setupMenu(Activity activity,
                                 LinearLayout navContainer,
                                 DrawerLayout drawerLayout,
                                 TextView mainContentText,
                                 int initialSelectedIndex,
                                 MenuSpec[] specs) {

        LayoutInflater inflater = LayoutInflater.from(activity);
        navContainer.removeAllViews();

        for (int i = 0; i < specs.length; i++) {
            MenuSpec spec = specs[i];

            View itemView = inflater.inflate(R.layout.nav_drawer_item, navContainer, false);
            ImageView icon = itemView.findViewById(R.id.nav_icon);
            TextView text = itemView.findViewById(R.id.nav_text);
            LinearLayout layout = itemView.findViewById(R.id.nav_item_layout);

            icon.setImageResource(spec.iconLight);
            text.setText(spec.label);

            // 초기 강조
            if (i == initialSelectedIndex) {
                icon.setImageResource(spec.iconDark);
                text.setTextColor(ContextCompat.getColor(activity, R.color.white));
                layout.setBackgroundColor(ContextCompat.getColor(activity, R.color.black));
                selectedItem = layout;
            }

            layout.setOnClickListener(v -> {
                // 이전 선택 해제
                if (selectedItem != null && selectedItem.getParent() instanceof ViewGroup) {
                    ViewGroup parent = (ViewGroup) selectedItem.getParent();
                    int prevIndex = parent.indexOfChild(selectedItem);
                    if (prevIndex >= 0 && prevIndex < specs.length) {
                        ImageView prevIcon = selectedItem.findViewById(R.id.nav_icon);
                        TextView prevText = selectedItem.findViewById(R.id.nav_text);
                        prevIcon.setImageResource(specs[prevIndex].iconLight);
                        prevText.setTextColor(ContextCompat.getColor(activity, R.color.black));
                        selectedItem.setBackgroundColor(ContextCompat.getColor(activity, R.color.gray));
                    }
                }

                // 현재 강조
                icon.setImageResource(spec.iconDark);
                text.setTextColor(ContextCompat.getColor(activity, R.color.white));
                layout.setBackgroundColor(ContextCompat.getColor(activity, R.color.black));
                selectedItem = layout;

                // 이동 처리
                boolean isSameActivity = (spec.targetActivity != null) && activity.getClass().equals(spec.targetActivity);
                if (spec.targetActivity != null && (spec.alwaysLaunchNew || !isSameActivity)) {
                    activity.startActivity(new Intent(activity, spec.targetActivity));
                } else if (mainContentText != null) {
                    mainContentText.setText(spec.label + " 화면입니다");
                }

                drawerLayout.closeDrawer(GravityCompat.START);
            });

            navContainer.addView(itemView);
        }
    }
}
