package com.example.qr.model.attendance;

public class AttendanceEntry {
    private String studentId;
    private String status;  // ✅ 출석 상태 (예: "출석", "결석", "지각")

    public String getStudentId() {
        return studentId;
    }

    public String getStatus() {
        return status;
    }
}
