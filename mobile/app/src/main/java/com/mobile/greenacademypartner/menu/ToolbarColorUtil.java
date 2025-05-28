package com.mobile.greenacademypartner.menu;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.mobile.greenacademypartner.R;

public class ToolbarColorUtil {

    public static void applyToolbarColor(Context context, Toolbar toolbar) {
        if (toolbar == null) return; // 안전장치

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        int defaultColor = ContextCompat.getColor(context, R.color.green);
        int savedColor = prefs.getInt("toolbar_color", defaultColor);
        toolbar.setBackgroundColor(savedColor);
    }

}
