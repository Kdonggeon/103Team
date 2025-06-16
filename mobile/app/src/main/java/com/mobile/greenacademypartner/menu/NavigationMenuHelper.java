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
import com.mobile.greenacademypartner.ui.MainActivity;
import com.mobile.greenacademypartner.ui.AttendanceActivity;
import com.mobile.greenacademypartner.ui.MyPageActivity;
import com.mobile.greenacademypartner.ui.NoticeActivity;
import com.mobile.greenacademypartner.ui.QAActivity;
import com.mobile.greenacademypartner.ui.SettingActivity;

public class NavigationMenuHelper {

    private static LinearLayout selectedItem = null;

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
            QAActivity.class,
            NoticeActivity.class,
            SettingActivity.class
    };

<<<<<<< HEAD
    public static void setupMenu(Activity activity, LinearLayout navContainer, DrawerLayout drawerLayout, TextView mainContentText, int initialSelectedIndex) {
=======
    public static void setupMenu(Activity activity, LinearLayout navContainer, DrawerLayout drawerLayout, TextView mainContentText) {
>>>>>>> sub
        LayoutInflater inflater = LayoutInflater.from(activity);

        for (int i = 0; i < labels.length; i++) {
            View itemView = inflater.inflate(R.layout.nav_drawer_item, navContainer, false);
            ImageView icon = itemView.findViewById(R.id.nav_icon);
            TextView text = itemView.findViewById(R.id.nav_text);
            LinearLayout layout = itemView.findViewById(R.id.nav_item_layout);

            icon.setImageResource(icons[i]);
            text.setText(labels[i]);

            int index = i;

<<<<<<< HEAD
            // ✅ 초기 선택 강조 처리
            if (i == initialSelectedIndex) {
                icon.setImageResource(icons_dark[i]);
                text.setTextColor(ContextCompat.getColor(activity, R.color.white));
                layout.setBackgroundColor(ContextCompat.getColor(activity, R.color.black));
                selectedItem = layout;
            }

            layout.setOnClickListener(v -> {
                // 이전 강조 제거
=======
            layout.setOnClickListener(v -> {
                // 이전 선택 초기화
>>>>>>> sub
                if (selectedItem != null) {
                    int prevIndex = ((ViewGroup) selectedItem.getParent()).indexOfChild(selectedItem);
                    ImageView prevIcon = selectedItem.findViewById(R.id.nav_icon);
                    TextView prevText = selectedItem.findViewById(R.id.nav_text);
                    prevIcon.setImageResource(icons[prevIndex]);
                    prevText.setTextColor(ContextCompat.getColor(activity, R.color.black));
                    selectedItem.setBackgroundColor(ContextCompat.getColor(activity, R.color.gray));
                }

<<<<<<< HEAD
                // 현재 강조 처리
=======
                // 현재 선택 강조
>>>>>>> sub
                icon.setImageResource(icons_dark[index]);
                text.setTextColor(ContextCompat.getColor(activity, R.color.white));
                layout.setBackgroundColor(ContextCompat.getColor(activity, R.color.black));
                selectedItem = layout;

<<<<<<< HEAD
                // ✅ 화면 전환 처리
                if (targetActivities[index] != null) {
                    boolean isAttendance = targetActivities[index] == AttendanceActivity.class;
                    boolean isSameActivity = activity.getClass().equals(targetActivities[index]);

                    // 출석관리 메뉴는 항상 새로 띄움, 나머지는 현재 화면이면 무시
                    if (isAttendance || !isSameActivity) {
                        activity.startActivity(new Intent(activity, targetActivities[index]));
                    } else {
                        if (mainContentText != null) {
                            mainContentText.setText(labels[index] + " 화면입니다");
                        }
=======
                // ✅ 중복 실행 방지: 현재 Activity가 아니면 전환
                if (targetActivities[index] != null && !activity.getClass().equals(targetActivities[index])) {
                    activity.startActivity(new Intent(activity, targetActivities[index]));
                } else {
                    if (mainContentText != null) {
                        mainContentText.setText(labels[index] + " 화면입니다");
>>>>>>> sub
                    }
                }

                drawerLayout.closeDrawer(GravityCompat.START);
            });

            navContainer.addView(itemView);
        }
    }
}
