package com.mobile.greenacademypartner.ui.setting;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.Deque;

public final class FontUtilSystem {

    private static final String PREFS = "settings_prefs";
    private static final String KEY_FONT = "sys_font_family_key";

    public enum SysFontKey {
        SANS,        // sans-serif
        SERIF,       // serif
        MONO,        // monospace
        SANS_MEDIUM  // sans-serif-medium (기기별 가용성 상이, 불가 시 SANS 대체)
    }

    private FontUtilSystem() {}

    public static void saveFont(Context ctx, SysFontKey key) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_FONT, key.name()).apply();
    }

    public static SysFontKey getSavedFont(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String v = sp.getString(KEY_FONT, SysFontKey.SANS.name());
        try {
            return SysFontKey.valueOf(v);
        } catch (IllegalArgumentException ex) {
            return SysFontKey.SANS;
        }
    }

    private static Typeface resolveTypeface(SysFontKey key) {
        switch (key) {
            case SERIF: return Typeface.SERIF;
            case MONO:  return Typeface.MONOSPACE;
            case SANS_MEDIUM: {
                Typeface t = Typeface.create("sans-serif-medium", Typeface.NORMAL);
                if (t != null) return t;
                return Typeface.SANS_SERIF;
            }
            case SANS:
            default:    return Typeface.SANS_SERIF;
        }
    }

    public static void apply(Activity activity) {
        SysFontKey key = getSavedFont(activity);
        Typeface tf = resolveTypeface(key);
        View root = activity.getWindow().getDecorView().getRootView();
        applyToTree(root, tf);
    }

    private static void applyToTree(View root, Typeface tf) {
        if (root == null) return;
        Deque<View> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            View v = q.removeFirst();
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                Typeface old = tv.getTypeface();
                int style = (old != null) ? old.getStyle() : Typeface.NORMAL;
                tv.setTypeface(tf, style);
            } else if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) {
                    q.addLast(g.getChildAt(i));
                }
            }
        }
    }
}
