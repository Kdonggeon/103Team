package com.mobile.greenacademypartner;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.mobile.greenacademypartner.api.ParentApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.StudentApi;
//import com.mobile.greenacademypartner.api.TeacherApi;

import retrofit2.Call;
import retrofit2.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
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

        if (role.equals("student")) {
            String studentId = prefs.getString("studentId", null);
            if (studentId != null) {
                StudentApi studentApi = RetrofitClient.getClient().create(StudentApi.class);
                studentApi.updateFcmToken(studentId, token).enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Log.d("FCM", "onNewToken() → 학생 토큰 서버 업데이트 성공");
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("FCM", "onNewToken() → 학생 토큰 서버 업데이트 실패", t);
                    }
                });
            }
//        } else if (role.equals("teacher")) {
//            String teacherId = prefs.getString("teacherId", null);
//            if (teacherId != null) {
//                TeacherApi teacherApi = RetrofitClient.getClient().create(TeacherApi.class);
//                teacherApi.updateFcmToken(teacherId, token).enqueue(new retrofit2.Callback<Void>() {
//                    @Override
//                    public void onResponse(Call<Void> call, Response<Void> response) {
//                        Log.d("FCM", "onNewToken() → 교사 토큰 서버 업데이트 성공");
//                    }
//                    @Override
//                    public void onFailure(Call<Void> call, Throwable t) {
//                        Log.e("FCM", "onNewToken() → 교사 토큰 서버 업데이트 실패", t);
//                    }
//                });
//            }
        } else if (role.equals("parent")) {
            String parentId = prefs.getString("parentId", null);
            if (parentId != null) {
                ParentApi parentApi = RetrofitClient
                        .getClient()
                        .create(ParentApi.class);
                parentApi.updateFcmToken(parentId, token)
                        .enqueue(new retrofit2.Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                                    Log.d("FCM", "onNewToken() → 부모 토큰 서버 업데이트 성공");}
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                                    Log.e("FCM", "onNewToken() → 부모 토큰 서버 업데이트 실패", t);
                                                }
                    });
            }

        } else {
            Log.d("FCM", "onNewToken() → 알 수 없는 역할: " + role);
        }
    }

}