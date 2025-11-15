package com.mobile.greenacademypartner.ui.setting;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobile.greenacademypartner.R;

public class ThemeColorUtil {
    public static void applyThemeColor(Activity activity) {
        applyThemeColor(activity, null); // toolbar 없는 경우
    }

    public static void applyThemeColor(Activity activity, Toolbar toolbar) {
        SharedPreferences prefs = activity.getSharedPreferences("settings", Activity.MODE_PRIVATE);
        int color = prefs.getInt("theme_color", activity.getColor(R.color.green));

        if (toolbar != null) {
            toolbar.setBackgroundColor(color);
        }

        applyColorToButtons(activity, color);
    }

    private static void applyColorToButtons(Activity activity, int color) {
        View root = activity.getWindow().getDecorView().getRootView();
        traverseAndColorButtons(root, color);
    }

    private static void traverseAndColorButtons(View view, int color) {

        // ✔ Button 이지만 RadioButton 은 제외!
        if (view instanceof Button && !(view instanceof RadioButton)) {
            ((Button) view).setBackgroundTintList(ColorStateList.valueOf(color));

        } else if (view instanceof FloatingActionButton) {
            ((FloatingActionButton) view).setBackgroundTintList(ColorStateList.valueOf(color));

        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                traverseAndColorButtons(group.getChildAt(i), color);
            }
        }
    }
}
