package com.team103.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;

    @Autowired
    public FcmService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    public void sendMessageTo(String targetToken, String title, String body) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(targetToken)
                    .setNotification(notification)
                    .build();

            String response = firebaseMessaging.send(message);
            System.out.println("[Firebase] 메시지 전송 완료: " + response);

        } catch (Exception e) {
            System.err.println("[Firebase] 메시지 전송 실패");
            e.printStackTrace();
        }
    }
}