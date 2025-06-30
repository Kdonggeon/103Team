package com.team103.dto;

public class LoginResponse {
    private String status;
    private String role;
    private String username;
    private String name;
    private String token;

    public LoginResponse() {
    }

    public LoginResponse(String status, String role, String username, String name, String token) {
        this.status = status;
        this.role = role;
        this.username = username;
        this.name = name;
        this.token = token;
    }

    public String getStatus() {
        return status;
    }

    public String getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getToken() {
        return token;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
