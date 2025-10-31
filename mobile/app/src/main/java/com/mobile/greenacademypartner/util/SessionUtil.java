package com.mobile.greenacademypartner.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.mobile.greenacademypartner.ui.login.LoginActivity;

import org.json.JSONObject;

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
                .remove("studentId")
                .apply();

        Intent i = new Intent(ctx, LoginActivity.class);
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
                        "studentId=" + p.getString("studentId", "") + "\n" +
                        "token.len=" + safe(p.getString("token", "")).length() + "\n" +
                        "accessToken.len=" + safe(p.getString("accessToken", "")).length()
        );
    }

    /* ==========================
       ✅ 추가: 학생 ID 조회
       우선순위: studentId 저장값 → role=student 이면 username → JWT(sub/studentId/uid/username)
       ========================== */
    public static String getStudentId(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 1) 명시 저장된 studentId
        String sid = sp.getString("studentId", null);
        if (notEmpty(sid)) return sid;

        // 2) role=student 이면 username을 학생ID로 사용
        String role = sp.getString("role", null);
        String username = sp.getString("username", null);
        if ("student".equalsIgnoreCase(role) && notEmpty(username)) {
            return username;
        }

        // 3) JWT 토큰에서 추출 (sub / studentId / uid / username)
        String token = sp.getString("token", null);
        if (!notEmpty(token)) token = sp.getString("accessToken", null);
        if (notEmpty(token)) {
            if (token.startsWith("Bearer ")) token = token.substring(7);
            String fromJwt = tryExtractStudentIdFromJwt(token);
            if (notEmpty(fromJwt)) return fromJwt;
        }

        return null; // 없으면 null
    }

    // ---------------- 내부 유틸 ----------------
    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String tryExtractStudentIdFromJwt(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;

            // URL-safe Base64 디코딩
            byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            String json = new String(decoded);
            JSONObject obj = new JSONObject(json);

            String[] keys = { "studentId", "sub", "uid", "username" };
            for (String k : keys) {
                if (obj.has(k)) {
                    String v = obj.optString(k, null);
                    if (notEmpty(v)) return v;
                }
            }
        } catch (Exception e) {
            Log.w("SessionUtil", "JWT 파싱 실패", e);
        }
        return null;
    }
}
