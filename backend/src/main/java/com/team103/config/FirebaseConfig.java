package com.team103.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true") // ✅ 필요할 때만 로드
public class FirebaseConfig {

    // ✅ application.yml 또는 환경변수로 넣을 경로
    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    @PostConstruct
    public void initialize() {
        try {
            if (credentialsPath == null || credentialsPath.isBlank()) {
                System.out.println("[Firebase] firebase.credentials-path 가 설정되지 않음. 초기화 생략");
                return;
            }

            if (!FirebaseApp.getApps().isEmpty()) {
                System.out.println("[Firebase] 이미 초기화되어 있음");
                return;
            }

            System.out.println("[Firebase] credentialsPath = " + credentialsPath);

            try (InputStream serviceAccount = new FileInputStream(credentialsPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("[Firebase] 초기화 완료");
            }
        } catch (Exception e) {
            System.err.println("[Firebase] 초기화 실패");
            e.printStackTrace();
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        // initialize() 에서 이미 DEFAULT 앱을 만들어주므로 여기서는 바로 인스턴스만 꺼냄
        return FirebaseMessaging.getInstance();
    }
}
