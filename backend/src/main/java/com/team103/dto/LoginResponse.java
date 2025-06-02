package com.team103.dto;

import lombok.AllArgsConstructor;
import lombok.Data;



public class LoginResponse {
    private String status;
    private String role;
    private String username;
    private String name;
    private String token; //토큰 발급

    public LoginResponse(String status, String role, String username, String name ,String token) {
        this.status = status;
        this.role = role;
        this.username = username;
        this.name = name;
        this.token = token;
    }

    // Getter/Setter도 필요하면 작성하거나 @Data 유지
    public String getStatus() { return status; }
    public String getRole() { return role; }
    public String getUsername() { return username; }
    public String getName() { return name; }
}

