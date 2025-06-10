package com.mobile.greenacademypartner.model;


public class LoginResponse {
    private String status;
    private String role;
    private String username;
    private String name;
    private String token;

    private String phone;

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

    public String getPhone() { return phone; }
}
