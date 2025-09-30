package com.mobile.greenacademypartner.ui.qna;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.mobile.greenacademypartner.R;

import com.mobile.greenacademypartner.api.RetrofitClient;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);
        Log.d("MyApplication", "Retrofit init"); // (선택) 확인 로그

        createNotificationChannel();
        registerFontOverlay();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "default_channel_id";
            String channelName = "기본 채널";
            String channelDescription = "앱 기본 알림 채널";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDescription);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private boolean shouldApplyFontOverlay(Activity a) {
        // 기본값은 "System" → 최초 진입은 시스템 기본 폰트
        String key = a.getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("app_font", "System");
        return "NotoSansKR".equals(key);
    }

    private int resolveFontOverlay() {
        // 현재 1종(Noto)만 사용
        return R.style.ThemeOverlay_GreenAcademy_Font_NotoSansKR;
    }

    private void registerFontOverlay() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityPreCreated(Activity activity, Bundle savedInstanceState) {
                if (Build.VERSION.SDK_INT >= 29 && shouldApplyFontOverlay(activity)) {
                    activity.getTheme().applyStyle(resolveFontOverlay(), true);
                }
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (Build.VERSION.SDK_INT < 29 && shouldApplyFontOverlay(activity)) {
                    boolean applied = activity.getIntent().getBooleanExtra("__font_applied__", false);
                    if (!applied) {
                        activity.getTheme().applyStyle(resolveFontOverlay(), true);
                        activity.getIntent().putExtra("__font_applied__", true);
                        activity.recreate(); // 오버레이 필요한 경우에만 1회 재생성
                    }
                }
            }

            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }
}
