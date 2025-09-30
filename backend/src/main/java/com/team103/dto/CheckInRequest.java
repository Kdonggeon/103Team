package com.team103.dto;

public class CheckInRequest {
    private String classId;
    private String studentId; 

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
}
