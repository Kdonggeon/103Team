package com.mobile.greenacademypartner.model.attendance;

public class AttendanceResponse {
    private String className;
    private String academyName; // ★ 학원명
    private String date;        // yyyy-MM-dd
    private String status;      // 출석/지각/결석

    public String getClassName() { return className; }
    public void setClassName(String v) { this.className = v; }

    public String getAcademyName() { return academyName; }
    public void setAcademyName(String v) { this.academyName = v; }

    public String getDate() { return date; }
    public void setDate(String v) { this.date = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}
