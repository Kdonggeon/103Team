package com.team103.dto;

public class CheckInRequest {
    private String classId;
    private String studentId;
    private Integer academyNumber; // ✅ 추가됨

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Integer getAcademyNumber() { return academyNumber; } // ✅ getter
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; } // ✅ setter
}
