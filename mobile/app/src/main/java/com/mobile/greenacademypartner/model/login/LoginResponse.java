package com.mobile.greenacademypartner.model.login;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class LoginResponse {

    @SerializedName("status")
    @Expose
    private String status;

    @SerializedName("role")
    @Expose
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

    // 학생 정보
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

    // 여러 학원
    @SerializedName("academyNumbers")
    @Expose
    private List<Integer> academyNumbers;

    // 학부모 전용
    @SerializedName("parentsNumber")
    @Expose
    private String parentsNumber;

    @SerializedName("childStudentId")
    @Expose
    private String childStudentId;

    // --- 기본 생성자 (Gson용) ---
    public LoginResponse() { }

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

    public List<Integer> getAcademyNumbers() { return academyNumbers; }
    public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }

    public String getParentsNumber() { return parentsNumber; }
    public void setParentsNumber(String parentsNumber) { this.parentsNumber = parentsNumber; }

    public String getChildStudentId() { return childStudentId; }
    public void setChildStudentId(String childStudentId) { this.childStudentId = childStudentId; }
}
