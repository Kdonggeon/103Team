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
import com.mobile.greenacademypartner.ui.director.DirectorMyPageActivity;
import com.mobile.greenacademypartner.ui.main.MainActivity;
import com.mobile.greenacademypartner.ui.attendance.AttendanceActivity;
import com.mobile.greenacademypartner.ui.mypage.MyPageActivity;
import com.mobile.greenacademypartner.ui.notice.NoticeActivity;
import com.mobile.greenacademypartner.ui.qna.QuestionsActivity;
import com.mobile.greenacademypartner.ui.setting.SettingActivity;

// ✅ 필요한 경우: 원장 전용 화면들 (임시로 클래명 예시)
//import com.mobile.greenacademypartner.ui.director.DirectorMyPageActivity;
//import com.mobile.greenacademypartner.ui.director.ManageTeachersActivity;
//import com.mobile.greenacademypartner.ui.director.ManageStudentsActivity;
//import com.mobile.greenacademypartner.ui.director.ManageParentsActivity;

public class NavigationMenuHelper {

    // ✅ 역할 구분
    public enum Role { STUDENT, TEACHER, PARENT, DIRECTOR }

    // ✅ 메뉴 스펙(한 항목)
    public static class MenuSpec {
        public final String label;
        public final int iconLight;
        public final int iconDark;
        public final Class<?> targetActivity;
        public final boolean alwaysLaunchNew; // 출석처럼 항상 새로 열고 싶을 때 true

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

    // ✅ 기존 API와 호환(역할 미지정 시 기존 배열 유지) -------------------------
    //    기존 호출부를 깨지 않기 위해 남겨둠.
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

    // 기존 호출 유지용 (학생/기본 메뉴)
    public static void setupMenu(Activity activity, LinearLayout navContainer, DrawerLayout drawerLayout, TextView mainContentText, int initialSelectedIndex) {
        // 기존 배열을 MenuSpec으로 변환해서 공통 메서드로 넘김
        MenuSpec[] specs = new MenuSpec[labels.length];
        for (int i = 0; i < labels.length; i++) {
            boolean alwaysNew = (targetActivities[i] == AttendanceActivity.class);
            specs[i] = new MenuSpec(labels[i], icons[i], icons_dark[i], targetActivities[i], alwaysNew);
        }
        setupMenu(activity, navContainer, drawerLayout, mainContentText, initialSelectedIndex, specs);
    }
    // --------------------------------------------------------------------

    // ✅ 역할별 메뉴 사양 팩토리
    public static MenuSpec[] getMenuForRole(Role role) {
        switch (role) {
            case TEACHER:
                // 필요 시 교사용 메뉴 정의
                return new MenuSpec[] {
                        new MenuSpec("My Page",    R.drawable.ic_person_light,     R.drawable.ic_person_dark,     MyPageActivity.class),
                        new MenuSpec("출석 관리",   R.drawable.ic_attendance_light, R.drawable.ic_attendance_dark, AttendanceActivity.class, true),
                        new MenuSpec("시간표",      R.drawable.ic_timetable_light,  R.drawable.ic_timetable_dark,  MainActivity.class),
                        new MenuSpec("Q&A",        R.drawable.ic_qa_light,         R.drawable.ic_qa_dark,         QuestionsActivity.class),
                        new MenuSpec("공지사항",    R.drawable.ic_notice_light,     R.drawable.ic_notice_dark,      NoticeActivity.class),
                        new MenuSpec("설정",        R.drawable.ic_settings_light,   R.drawable.ic_settings_dark,    SettingActivity.class)
                };

            case PARENT:
            case STUDENT:
            default:
                // 기본(현재 구현된 배열과 동일)
                MenuSpec[] defaultSpecs = new MenuSpec[labels.length];
                for (int i = 0; i < labels.length; i++) {
                    boolean alwaysNew = (targetActivities[i] == AttendanceActivity.class);
                    defaultSpecs[i] = new MenuSpec(labels[i], icons[i], icons_dark[i], targetActivities[i], alwaysNew);
                }
                return defaultSpecs;
        }
    }

    // ✅ 공통 렌더러 (역할/스펙 기반)
    public static void setupMenu(Activity activity,
                                 LinearLayout navContainer,
                                 DrawerLayout drawerLayout,
                                 TextView mainContentText,
                                 int initialSelectedIndex,
                                 Role role) {
        setupMenu(activity, navContainer, drawerLayout, mainContentText, initialSelectedIndex, getMenuForRole(role));
    }

    // 실제 구현(모든 케이스 공통)
    public static void setupMenu(Activity activity,
                                 LinearLayout navContainer,
                                 DrawerLayout drawerLayout,
                                 TextView mainContentText,
                                 int initialSelectedIndex,
                                 MenuSpec[] specs) {

        LayoutInflater inflater = LayoutInflater.from(activity);
        navContainer.removeAllViews(); // 중복 추가 방지

        for (int i = 0; i < specs.length; i++) {
            MenuSpec spec = specs[i];

            View itemView = inflater.inflate(R.layout.nav_drawer_item, navContainer, false);
            ImageView icon = itemView.findViewById(R.id.nav_icon);
            TextView text = itemView.findViewById(R.id.nav_text);
            LinearLayout layout = itemView.findViewById(R.id.nav_item_layout);

            icon.setImageResource(spec.iconLight);
            text.setText(spec.label);

            int index = i;

            // ✅ 초기 강조
            if (i == initialSelectedIndex) {
                icon.setImageResource(spec.iconDark);
                text.setTextColor(ContextCompat.getColor(activity, R.color.white));
                layout.setBackgroundColor(ContextCompat.getColor(activity, R.color.black));
                selectedItem = layout;
            }

            layout.setOnClickListener(v -> {
                // 이전 선택 해제
                if (selectedItem != null) {
                    int prevIndex = ((ViewGroup) selectedItem.getParent()).indexOfChild(selectedItem);
                    ImageView prevIcon = selectedItem.findViewById(R.id.nav_icon);
                    TextView prevText = selectedItem.findViewById(R.id.nav_text);
                    prevIcon.setImageResource(specs[prevIndex].iconLight);
                    prevText.setTextColor(ContextCompat.getColor(activity, R.color.black));
                    selectedItem.setBackgroundColor(ContextCompat.getColor(activity, R.color.gray));
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
                } else {
                    if (mainContentText != null) {
                        mainContentText.setText(spec.label + " 화면입니다");
                    }
                }

                drawerLayout.closeDrawer(GravityCompat.START);
            });

            navContainer.addView(itemView);
        }
    }
}
