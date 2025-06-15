package com.team103.model;

import org.springframework.data.mongodb.core.mapping.Field;

public class AttendanceEntry {

    @Field("Student_ID")
    private String studentId;

    @Field("Status")
    private String status; // 출석, 결석, 지각 등

    public AttendanceEntry() {}

    public AttendanceEntry(String studentId, String status) {
        this.studentId = studentId;
        this.status = status;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
