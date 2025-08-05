package com.team103.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    private static final String FIREBASE_CONFIG_RELATIVE_PATH = "src/main/resources/firebase/firebase-adminsdk.json";

    @PostConstruct
    public void initialize() {
        try (FileInputStream serviceAccount = new FileInputStream(FIREBASE_CONFIG_RELATIVE_PATH)) {

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("[Firebase] 초기화 완료");
            } else {
                System.out.println("[Firebase] 이미 초기화되어 있습니다.");
            }

        } catch (IOException e) {
            System.err.println("[Firebase] 초기화 실패");
            e.printStackTrace();
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}
