package com.team103.dto;

public class StudentUpdateRequest {

    private String studentId;
    private String studentName;
    private String studentPhoneNumber;
    private String address;
    private String school;

    private Integer grade;          // ← int → Integer 변경 (null 허용)
    private String gender;

    private String parentId;        // ← ★ 추가
    private String parentsNumber;   // ← ★ 추가

    public StudentUpdateRequest() {}

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentPhoneNumber() { return studentPhoneNumber; }
    public void setStudentPhoneNumber(String studentPhoneNumber) { this.studentPhoneNumber = studentPhoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getParentsNumber() { return parentsNumber; }
    public void setParentsNumber(String parentsNumber) { this.parentsNumber = parentsNumber; }
}
