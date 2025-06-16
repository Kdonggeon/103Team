package com.mobile.greenacademypartner.model;

public class PasswordResetRequest {
    private String role;
    private String id;
    private String name;
    private String phone; // ✅ long → String
    private String newPassword;

    public PasswordResetRequest(String role, String id, String name, String phone, String newPassword) {
        this.role = role;
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.newPassword = newPassword;
    }

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
