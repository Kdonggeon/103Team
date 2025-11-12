package com.example.qr.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.qr.MainActivity;
import com.example.qr.R;
import com.example.qr.api.RetrofitClient;
import com.example.qr.api.StudentApi;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // 알림 허용 여부 체크 (선택사항)
        SharedPreferences login = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String uid = login.getString("username", "");
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        boolean enabled = settings.getBoolean("notifications_enabled_" + uid, true);
        if (!enabled) return;

        String title = remoteMessage.getData().get("title");
        String body  = remoteMessage.getData().get("body");

        if (title == null && remoteMessage.getNotification() != null)
            title = remoteMessage.getNotification().getTitle();
        if (body == null && remoteMessage.getNotification() != null)
            body = remoteMessage.getNotification().getBody();

        if (title == null) title = "새 알림";
        if (body  == null) body  = "내용 없음";

        showNotification(title, body);
    }

    private void showNotification(String title, String message) {
        String channelId = "default_channel_id";
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    channelId, "기본 채널", NotificationManager.IMPORTANCE_HIGH
            );
            nm.createNotificationChannel(ch);
        }

        // 알림 클릭 시 MainActivity 열기
        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        nm.notify((int) System.currentTimeMillis(), b.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "새 토큰 발급: " + token);

        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);

        // 학생 전용 처리만 남김
        String jwt = prefs.getString("token", null);
        String studentId = firstNonEmpty(
                prefs.getString("studentId", null),
                prefs.getString("userId", null),
                prefs.getString("username", null)
        );

        if (jwt == null || jwt.trim().isEmpty() || studentId == null || studentId.trim().isEmpty()) {
            Log.w("FCM", "필수 정보 부족 → 서버 업로드 스킵 (jwt or studentId 없음)");
            return;
        }

        String authHeader = "Bearer " + jwt.trim();

        StudentApi api = RetrofitClient.getClient().create(StudentApi.class);
        api.updateFcmToken(studentId, authHeader, token)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> res) {
                        if (res.isSuccessful()) {
                            Log.d("FCM", "학생 FCM 토큰 서버 업데이트 성공");
                        } else {
                            Log.e("FCM", "학생 FCM 토큰 서버 업데이트 실패 code=" + res.code());
                        }
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("FCM", "학생 FCM 토큰 서버 네트워크 실패", t);
                    }
                });
    }

    private String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) if (v != null && !v.trim().isEmpty()) return v;
        return null;
    }
}
