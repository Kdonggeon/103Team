package com.team103.dto;

public class LoginResponse {

    private String status;
    private String role;
    private String username;
    private String name;
    private String token;

    // ✅ 학생용 필드
    private String phone;
    private String address;
    private String school;
    private int grade;
    private String gender;

    // 기본 생성자
    public LoginResponse() {}

    // 🔹 기존: 공통 로그인용
    public LoginResponse(String status, String role, String username, String name, String token) {
        this.status = status;
        this.role = role;
        this.username = username;
        this.name = name;
        this.token = token;
    }

    // 🔹 학생 전용: 전체 정보 포함
    public LoginResponse(String status, String role, String username, String name, String token,
                         String phone, String address, String school, int grade, String gender) {
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
    }

    // --- Getters ---
    public String getStatus() { return status; }
    public String getRole() { return role; }
    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getToken() { return token; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getSchool() { return school; }
    public int getGrade() { return grade; }
    public String getGender() { return gender; }

    // --- Setters (optional) ---
    public void setStatus(String status) { this.status = status; }
    public void setRole(String role) { this.role = role; }
    public void setUsername(String username) { this.username = username; }
    public void setName(String name) { this.name = name; }
    public void setToken(String token) { this.token = token; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setSchool(String school) { this.school = school; }
    public void setGrade(int grade) { this.grade = grade; }
    public void setGender(String gender) { this.gender = gender; }
}
