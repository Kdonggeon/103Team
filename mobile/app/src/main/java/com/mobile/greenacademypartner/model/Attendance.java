package com.mobile.greenacademypartner.model;

public class Attendance {

    private String id;
    private String studentId;
    private String classId;
    private String status;  // 예: "출석", "결석"
    private String date;

    public String getStudentId() { return studentId; }
    public String getClassId() { return classId; }
    public String getStatus() { return status; }
    public String getDate() { return date; }

    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setClassId(String classId) { this.classId = classId; }
    public void setStatus(String status) { this.status = status; }
    public void setDate(String date) { this.date = date; }
}
