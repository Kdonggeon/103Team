package com.team103.dto;

import java.util.List;

public class LoginResponse {

    private String status;
    private String role;
    private String username;
    private String name;
    private String token;
    private String phone;

    // ✅ 학생용 필드
    private String address;
    private String school;
    private int grade;
    private String gender;

    // ✅ 교사/학생/학부모 공통 필드 (학원 여러 개)
    private List<Integer> academyNumbers;

    // ✅ 학부모용 필드
    private String parentsNumber;
    private String childStudentId; // ← 자녀의 studentId

    // --- 기본 생성자 ---
    public LoginResponse() {}

    // --- ✅ 리스트 기반 통합 생성자 ---
    public LoginResponse(String status, String role, String username, String name,
                         String token, String phone, String address, String school,
                         int grade, String gender, List<Integer> academyNumbers) {
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
        this.academyNumbers = academyNumbers;
    }

    // --- Getter ---
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
    public List<Integer> getAcademyNumbers() { return academyNumbers; }
    public String getParentsNumber() { return parentsNumber; }
    public String getChildStudentId() { return childStudentId; }

    // --- Setter ---
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
    public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }
    public void setParentsNumber(String parentsNumber) { this.parentsNumber = parentsNumber; }
    public void setChildStudentId(String childStudentId) { this.childStudentId = childStudentId; }
}
