package com.team103.dto;

public class PasswordResetRequest {
    private String role; // "student", "teacher", "parent"
    private String id;
    private String name;
    private String phone;
    private String newPassword;

    // Getters & Setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
