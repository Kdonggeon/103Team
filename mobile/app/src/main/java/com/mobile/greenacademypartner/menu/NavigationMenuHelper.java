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

    public static void setupMenu(Activity activity, LinearLayout navContainer, DrawerLayout drawerLayout, TextView mainContentText, int initialSelectedIndex) {
        LayoutInflater inflater = LayoutInflater.from(activity);

        for (int i = 0; i < labels.length; i++) {
            View itemView = inflater.inflate(R.layout.nav_drawer_item, navContainer, false);
            ImageView icon = itemView.findViewById(R.id.nav_icon);
            TextView text = itemView.findViewById(R.id.nav_text);
            LinearLayout layout = itemView.findViewById(R.id.nav_item_layout);

            icon.setImageResource(icons[i]);
            text.setText(labels[i]);

            int index = i;

            // ✅ 초기 강조
            if (i == initialSelectedIndex) {
                icon.setImageResource(icons_dark[i]);
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
                    prevIcon.setImageResource(icons[prevIndex]);
                    prevText.setTextColor(ContextCompat.getColor(activity, R.color.black));
                    selectedItem.setBackgroundColor(ContextCompat.getColor(activity, R.color.gray));
                }

                // 현재 강조
                icon.setImageResource(icons_dark[index]);
                text.setTextColor(ContextCompat.getColor(activity, R.color.white));
                layout.setBackgroundColor(ContextCompat.getColor(activity, R.color.black));
                selectedItem = layout;

                // ✅ 출석관리는 항상 새로 열고, 나머지는 동일 액티비티면 무시
                boolean isAttendance = targetActivities[index] == AttendanceActivity.class;
                boolean isSameActivity = activity.getClass().equals(targetActivities[index]);

                if (targetActivities[index] != null && (isAttendance || !isSameActivity)) {
                    activity.startActivity(new Intent(activity, targetActivities[index]));
                } else {
                    if (mainContentText != null) {
                        mainContentText.setText(labels[index] + " 화면입니다");
                    }
                }

                drawerLayout.closeDrawer(GravityCompat.START);
            });

            navContainer.addView(itemView);
        }
    }
}
