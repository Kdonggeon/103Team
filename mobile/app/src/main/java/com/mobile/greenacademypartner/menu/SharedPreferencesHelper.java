package com.mobile.greenacademypartner.menu;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.mobile.greenacademypartner.R;

public class SharedPreferencesHelper {
    private static final String PREF_NAME = "app_preferences";
    private static final String KEY_TOOLBAR_COLOR = "toolbar_color";

    public static void saveToolbarColor(Context context, int color) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_TOOLBAR_COLOR, color).apply();
    }

    public static int getToolbarColor(Context context, int defaultColor) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_TOOLBAR_COLOR, defaultColor);
    }


}
