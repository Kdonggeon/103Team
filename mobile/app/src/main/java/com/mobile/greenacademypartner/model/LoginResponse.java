package com.mobile.greenacademypartner.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("status")

    private String status;

    @SerializedName("role")
    private String role;

    @SerializedName("username")
    @Expose
    private String username;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("token")
    @Expose
    private String token;

    @SerializedName("phone")
    @Expose
    private String phone;

    @SerializedName("address")
    @Expose
    private String address;

    @SerializedName("school")
    @Expose
    private String school;

    @SerializedName("grade")
    @Expose
    private int grade;

    @SerializedName("gender")
    @Expose
    private String gender;

    @SerializedName("academyNumber")
    @Expose
    private int academyNumber;

    // --- Getter / Setter ---
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }

    public LoginResponse(String status, String role, String username, String name,
                         String token, String phone, String address, String school,
                         int grade, String gender, int academyNumber) {
        this.status = status;
        this.role = role;
        this.username = username;
        this.name = name;
        this.token = token;
        this.phone = phone;
        this.address = address;
        this.school = school;
        this.grade = grade;
        this.gender = gender;
        this.academyNumber = academyNumber;
    }

}
