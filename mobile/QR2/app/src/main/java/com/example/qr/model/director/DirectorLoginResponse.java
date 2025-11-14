package com.example.qr.model.director;

public class DirectorLoginResponse {

    private String status;   // ex: "success"
    private String role;     // ex: "director"
    private String token;    // JWT 토큰

    public String getStatus() {
        return status;
    }

    public String getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }
}
