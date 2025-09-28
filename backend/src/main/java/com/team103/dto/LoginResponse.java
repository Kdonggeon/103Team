package com.team103.dto;

import java.util.List;

public class LoginResponse {
    private String status;           // "success" / "fail"
    private String role;             // "student" | "teacher" | "parent" | "director"
    private String username;         // 로그인 ID (studentId, teacherId, parentsId, director username 등)
    private String name;             // 표시 이름
    private String token;            // JWT 등
    private String phone;            // 선택
    private String address;          // 선택 (학생)
    private String school;           // 선택 (학생)
    private Integer grade;           // 선택 (학생)
    private String gender;           // 선택 (학생)
    private List<Integer> academyNumbers; // 공통(여러 역할에서 사용)
    private String parentsNumber;   // 학부모 고유 번호(선택)
    private String childStudentId;   // 학부모: 첫 번째 자녀 ID(선택)

    public LoginResponse() { }

    public LoginResponse(
            String status,
            String role,
            String username,
            String name,
            String token,
            String phone,
            String address,
            String school,
            Integer grade,
            String gender,
            List<Integer> academyNumbers
    ) {
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

    // 선택 필드 세터(체이닝은 안 써도 됨)
    public void setParentsNumber(String string) { this.parentsNumber = string; }
    public void setChildStudentId(String childStudentId) { this.childStudentId = childStudentId; }

    // Getters/Setters (필요한 만큼 생성)
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

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public List<Integer> getAcademyNumbers() { return academyNumbers; }
    public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }

    public String getParentsNumber() { return parentsNumber; }
    public String getChildStudentId() { return childStudentId; }
}
