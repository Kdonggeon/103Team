package com.example.qr.model.login;

public class LoginRequest {
    private String username;
    private String password;
    private String fcmToken;

    // ✅ 모든 필드 포함 생성자
    public LoginRequest(String username, String password, String fcmToken) {
        this.username = username;
        this.password = password;
        this.fcmToken = fcmToken;
    }

    // ✅ 로그인 시 FCM 토큰이 아직 필요 없다면, 이 오버로딩 생성자 사용
    public LoginRequest(String username, String password) {
        this(username, password, null);
    }

    // ✅ Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFcmToken() { return fcmToken; }

    // ✅ Setters
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}
