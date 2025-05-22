package com.mobile.greenacademypartner.menu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mobile.greenacademypartner.R;
import androidx.core.content.ContextCompat;


public class NavigationMenuHelper {
    // 선택 추적 변수 추가
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
    // 다크 아이콘 추가
    private static final int[] icons_dark = {
            R.drawable.ic_person_dark,
            R.drawable.ic_attendance_dark,
            R.drawable.ic_timetable_dark,
            R.drawable.ic_qa_dark,
            R.drawable.ic_notice_dark,
            R.drawable.ic_settings_dark
    };

    public static void setupMenu(Context context, LinearLayout navContainer) {
        LayoutInflater inflater = LayoutInflater.from(context);

        for (int i = 0; i < labels.length; i++) {
            View itemView = inflater.inflate(R.layout.nav_drawer_item, navContainer, false);
            ImageView icon = itemView.findViewById(R.id.nav_icon);
            TextView text = itemView.findViewById(R.id.nav_text);
            LinearLayout layout = itemView.findViewById(R.id.nav_item_layout);

            icon.setImageResource(icons[i]);
            text.setText(labels[i]);

            int index = i;
            layout.setOnClickListener(v -> {
                if (selectedItem != null) {
                    int prevIndex = ((ViewGroup) selectedItem.getParent()).indexOfChild(selectedItem);
                    ImageView prevIcon = selectedItem.findViewById(R.id.nav_icon);
                    TextView prevText = selectedItem.findViewById(R.id.nav_text);
                    prevIcon.setImageResource(icons[prevIndex]);
                    prevText.setTextColor(ContextCompat.getColor(context, R.color.black));  // ← 텍스트 색
                    selectedItem.setBackgroundColor(ContextCompat.getColor(context, R.color.gray));  // ← 이전 항목 배경색
                }

                icon.setImageResource(icons_dark[index]);
                text.setTextColor(ContextCompat.getColor(context, R.color.white));  // ← 선택된 텍스트 색
                layout.setBackgroundColor(ContextCompat.getColor(context, R.color.black));  // ← 선택된 배경색
                selectedItem = layout;


//                if (context instanceof android.app.Activity) {
//                    android.widget.Toast.makeText(context, labels[index] + " 클릭됨", android.widget.Toast.LENGTH_SHORT).show();
//                }
            });

            navContainer.addView(itemView);
        }
    }
}
