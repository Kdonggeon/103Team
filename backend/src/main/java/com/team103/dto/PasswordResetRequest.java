// src/main/java/com/team103/dto/PasswordResetRequest.java
package com.team103.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PasswordResetRequest {

    @JsonProperty("role")
    @JsonAlias({"Role"})
    private String role; // "student", "teacher", "parent", "director" 도 허용

    @JsonProperty("id")
    @JsonAlias({
        "username",           // 프론트에서 username으로 보낼 때
        "Student_ID", "Parents_ID", "Teacher_ID", "Director_ID"
    })
    private String id;

    @JsonProperty("name")
    @JsonAlias({
        "Student_Name", "Parents_Name", "Teacher_Name", "Director_Name"
    })
    private String name;

    @JsonProperty("phone")
    @JsonAlias({
        "phoneNumber",        // 프론트에서 phoneNumber로 보낼 때
        "studentPhoneNumber", "Parents_Phone_Number", "Teacher_Phone_Number", "Director_Phone_Number"
    })
    private String phone;

    @JsonProperty("newPassword")
    @JsonAlias({"password"})
    private String newPassword;

    // ----- Getters & Setters -----
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

    /** 전화번호 비교용(숫자만 남김) */
    public String normalizedPhone() {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }
}
