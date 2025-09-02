package com.mobile.greenacademypartner.menu;

import android.graphics.Color;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;

public class ToolbarIconUtil {

    // 항상 햄버거/뒤로가기/⋮ 메뉴 아이콘을 흰색으로 고정
    public static void applyWhiteIcons(Toolbar toolbar, ActionBarDrawerToggle toggle) {
        int white = Color.WHITE;

        if (toggle != null) {
            toggle.getDrawerArrowDrawable().setColor(white);
        }
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(white);
        }
        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setTint(white);
        }

        toolbar.setTitleTextColor(white);
        toolbar.setSubtitleTextColor(white);
    }
}
