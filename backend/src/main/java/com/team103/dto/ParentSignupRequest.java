package com.team103.dto;

import com.team103.model.Parent;

public class ParentSignupRequest {

    private String username;
    private String password;
    private String name;
    private String phoneNumber;

    public Parent toEntity(String encodedPw) {
        return new Parent(username, encodedPw, name, phoneNumber);
    }

    // 직접 작성한 Getter & Setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
