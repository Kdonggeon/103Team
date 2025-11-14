package com.example.qr.model.director;

public class DirectorLoginRequest {

    private String username;
    private String password;

    public DirectorLoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
