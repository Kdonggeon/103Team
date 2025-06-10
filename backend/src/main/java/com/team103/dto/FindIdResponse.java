package com.team103.dto;

public class FindIdResponse {
    private String role;
    private String userId;

    public FindIdResponse(String role, String userId) {
        this.role = role;
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public String getUserId() {
        return userId;
    }
}
