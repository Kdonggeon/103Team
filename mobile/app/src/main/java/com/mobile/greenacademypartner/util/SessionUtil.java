package com.mobile.greenacademypartner.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import com.mobile.greenacademypartner.ui.login.LoginActivity;

public final class SessionUtil {
    private SessionUtil() {}

    public static final String PREFS_NAME = "login_prefs";

    /** null-safe trim */
    public static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    /** 네트워크 연결 여부 (SDK 23↓ 호환) */
    public static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return caps != null && (
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            @SuppressWarnings("deprecation")
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    /** 로그인 정보 초기화 후 LoginActivity로 이동(태스크 클리어) */
    public static void clearLoginAndGoLogin(Context ctx, String reason) {
        Log.w("SessionUtil", "Force logout: " + reason);
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean("is_logged_in", false)
                .remove("token")
                .remove("accessToken")
                .remove("role")
                .remove("username")
                .apply();

        Intent i = new Intent(ctx, LoginActivity.class);
        // Task 비우고 로그인으로 진입
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ctx.startActivity(i);
    }

    /** 디버깅용: 현재 저장값 로그로 덤프 */
    public static void debugDumpPrefs(Context ctx, String tag) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(tag,
                "is_logged_in=" + p.getBoolean("is_logged_in", false) + "\n" +
                        "auto_login=" + p.getBoolean("auto_login", false) + "\n" +
                        "username=" + p.getString("username", "") + "\n" +
                        "role=" + p.getString("role", "") + "\n" +
                        "token.len=" + safe(p.getString("token", "")).length() + "\n" +
                        "accessToken.len=" + safe(p.getString("accessToken", "")).length()
        );
    }
}
