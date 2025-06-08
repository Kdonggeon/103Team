package com.team103.dto;

import com.team103.model.Teacher;

public class TeacherSignupRequest {
    private String username;
    private String password;
    private String name;
    private String phoneNumber;

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setName(String name) { this.name = name; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Teacher toEntity(String encodedPw) {
        return new Teacher(username, encodedPw, name, phoneNumber);
    }
}
