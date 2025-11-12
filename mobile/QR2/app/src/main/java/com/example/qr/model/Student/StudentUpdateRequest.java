package com.example.qr.model.Student;

public class StudentUpdateRequest {
    private String studentId;
    private String studentName;
    private String studentPhoneNumber;
    private String studentAddress;
    private String school;
    private int grade;
    private String gender;

    // ✅ 부모 식별자 (학부모 계정으로 수정 시 필요)
    private String parentId;

    // 기본 생성자
    public StudentUpdateRequest() {}

    // 전체 생성자
    public StudentUpdateRequest(String studentId, String studentName, String studentPhoneNumber,
                                String studentAddress, String school, int grade, String gender) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentPhoneNumber = studentPhoneNumber;
        this.studentAddress = studentAddress;
        this.school = school;
        this.grade = grade;
        this.gender = gender;
    }

    // --- Getters ---
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getStudentPhoneNumber() { return studentPhoneNumber; }
    public String getStudentAddress() { return studentAddress; }
    public String getSchool() { return school; }
    public int getGrade() { return grade; }
    public String getGender() { return gender; }
    public String getParentId() { return parentId; }

    // --- Setters ---
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setStudentPhoneNumber(String studentPhoneNumber) { this.studentPhoneNumber = studentPhoneNumber; }
    public void setStudentAddress(String studentAddress) { this.studentAddress = studentAddress; }
    public void setSchool(String school) { this.school = school; }
    public void setGrade(int grade) { this.grade = grade; }
    public void setGender(String gender) { this.gender = gender; }
    public void setParentId(String parentId) { this.parentId = parentId; }
}
