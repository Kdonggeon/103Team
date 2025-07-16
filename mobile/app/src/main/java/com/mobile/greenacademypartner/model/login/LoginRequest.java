package com.mobile.greenacademypartner.model.login;

public class LoginRequest {
    private String username;
    private String password;
    private String fcmToken;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
        this.fcmToken = fcmToken;
    }

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFcmToken() { return fcmToken; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}

