package com.team103.dto;

public class FcmTokenRequest {

    private String userId;
    private String token;

    public FcmTokenRequest() {
        // 기본 생성자 (JSON 직렬화/역직렬화에 필요)
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
