package com.team103.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    @PostConstruct
    public void initialize() {
        try {
            // 경로의 classpath 접두사 제거 후 실제 리소스 파일 열기
            String resourcePath = firebaseConfigPath.replace("classpath:", "");
            InputStream serviceAccount = getClass()
                    .getClassLoader()
                    .getResourceAsStream(resourcePath);

            if (serviceAccount == null) {
                throw new IllegalStateException("[Firebase] 서비스 계정 키 파일을 찾을 수 없습니다: " + resourcePath);
            }

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("[Firebase] 초기화 완료");
            } else {
                System.out.println("[Firebase] 이미 초기화되어 있습니다.");
            }

        } catch (Exception e) {
            System.err.println("[Firebase] 초기화 실패");
            e.printStackTrace();
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}
