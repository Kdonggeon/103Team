// ForceLogoutHelper.java
package com.mobile.greenacademypartner.core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.mobile.greenacademypartner.ui.login.LoginActivity;

public class ForceLogoutHelper {
    public static final String PREFS_NAME = "login_prefs";
    public static final String ACTION_FORCE_LOGOUT = "com.mobile.greenacademypartner.FORCE_LOGOUT";

    public static void forceLogout(Context ctx, String reason) {
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putBoolean("is_logged_in", false)
                    .remove("token")
                    .remove("accessToken")
                    .remove("username")
                    .remove("role")
                    .apply();

            if (reason != null && !reason.isEmpty()) {
                Toast.makeText(ctx, reason, Toast.LENGTH_SHORT).show();
            }

            Intent i = new Intent(ctx, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ctx.startActivity(i);
        } catch (Exception ignored) {}
    }
}
