package com.mobile.greenacademypartner;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.api.StudentApi;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        SharedPreferences login = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String uid = login.getString("username", "");
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        boolean enabled = settings.getBoolean("notifications_enabled_" + uid, true);
        if (!enabled) {
            // 알림 끈 상태면 그냥 표시 안 함
            return;
        }
        Log.d("FCM", "onMessageReceived() 진입: " + remoteMessage.getData());

        String title = remoteMessage.getData().get("title");
        String body  = remoteMessage.getData().get("body");

        if (title == null && remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
        }
        if (body == null && remoteMessage.getNotification() != null) {
            body = remoteMessage.getNotification().getBody();
        }
        if (title == null) title = "새 알림";
        if (body  == null) body  = "내용 없음";

        Log.d("FCM", "알림 제목: " + title + " / 내용: " + body);
        showNotification(title, body);
    }

    private void showNotification(String title, String message) {
        String channelId = "default_channel_id";
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "기본 채널",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "onNewToken() → 새 토큰: " + token);

        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", null);

        if (role == null) {
            Log.d("FCM", "역할 정보 없음 → 서버 갱신 스킵");
            return;
        }

        // JWT 읽어와서 Authorization 헤더 준비
        String jwt = prefs.getString("token", null);
        if (jwt == null || jwt.trim().isEmpty()) {
            Log.w("FCM", "JWT 없음 → 서버에 FCM 토큰 업로드 못 함");
            return;
        }
        String authHeader = "Bearer " + jwt.trim();

        // 공통 폴백 username
        String username = firstNonEmpty(
                prefs.getString("userId", null),
                prefs.getString("username", null)
        );

        if ("student".equalsIgnoreCase(role)) {
            String id = firstNonEmpty(
                    prefs.getString("studentId", null),
                    username
            );
            if (id != null && !id.trim().isEmpty()) {
                StudentApi studentApi = RetrofitClient.getClient().create(StudentApi.class);
                studentApi.updateFcmToken(
                        id,          // @Path("studentId")
                        authHeader,  // @Header("Authorization")
                        token        // @Body String fcmToken
                ).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d("FCM", "onNewToken() → 학생 토큰 서버 업데이트 성공");
                        } else {
                            Log.e("FCM", "onNewToken() → 학생 토큰 서버 업데이트 실패 code=" + response.code());
                        }
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("FCM", "onNewToken() → 학생 토큰 서버 업데이트 네트워크 실패", t);
                    }
                });
            } else {
                Log.w("FCM", "student 역할인데 studentId/username 없음 → 업로드 스킵");
            }

        } else if ("parent".equalsIgnoreCase(role)) {
            String id = firstNonEmpty(
                    prefs.getString("parentId", null),
                    username
            );
            if (id != null && !id.trim().isEmpty()) {
                ParentApi parentApi = RetrofitClient.getClient().create(ParentApi.class);
                parentApi.updateFcmToken(
                        id,          // @Path("id")
                        authHeader,  // @Header("Authorization")
                        token        // @Body String fcmToken
                ).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d("FCM", "onNewToken() → 부모 토큰 서버 업데이트 성공");
                        } else {
                            Log.e("FCM", "onNewToken() → 부모 토큰 서버 업데이트 실패 code=" + response.code());
                        }
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("FCM", "onNewToken() → 부모 토큰 서버 업데이트 네트워크 실패", t);
                    }
                });
            } else {
                Log.w("FCM", "parent 역할인데 parentId/username 없음 → 업로드 스킵");
            }

        } else {
            Log.d("FCM", "onNewToken() → 알 수 없는 역할: " + role);
        }
    }

    private String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) return v;
        }
        return null;
    }
}