package com.mobile.greenacademypartner.ui.setting;


import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.appcompat.widget.Toolbar;

public class ThemeColorUtil {
    public static void applyThemeColor(Activity activity) {
        applyThemeColor(activity, null); // toolbar 없는 경우
    }

    public static void applyThemeColor(Activity activity, Toolbar toolbar) {
        SharedPreferences prefs = activity.getSharedPreferences("settings", Activity.MODE_PRIVATE);
        int color = prefs.getInt("theme_color", activity.getColor(com.mobile.greenacademypartner.R.color.green));

        if (toolbar != null) {
            toolbar.setBackgroundColor(color);
        }

        // 버튼 등 전체 적용
        applyColorToButtons(activity, color);
    }

    private static void applyColorToButtons(Activity activity, int color) {
        View root = activity.getWindow().getDecorView().getRootView();
        traverseAndColorButtons(root, color);
    }

    private static void traverseAndColorButtons(View view, int color) {
        if (view instanceof Button) {
            ((Button) view).setBackgroundColor(color);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                traverseAndColorButtons(group.getChildAt(i), color);
            }
        }
    }
}

