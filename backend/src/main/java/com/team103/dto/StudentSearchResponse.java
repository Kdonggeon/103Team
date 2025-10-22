package com.team103.dto;

public class StudentSearchResponse {
    private String studentId;
    private String studentName;
    private Integer grade;     // 선택: 학년 (나이 대신 프록시)
    private Integer academyNumber;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }
}
