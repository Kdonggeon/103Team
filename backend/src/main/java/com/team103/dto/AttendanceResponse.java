package com.team103.dto;

public class AttendanceResponse {
    private String className;
    private String academyName; // 학원명
    private String date;        // yyyy-MM-dd
    private String status;      // 출석/지각/결석

    // ★ 기본 생성자 (무인자)
    public AttendanceResponse() {}

    // ★ 전체 필드 생성자 (편의용)
    public AttendanceResponse(String className, String academyName, String date, String status) {
        this.className = className;
        this.academyName = academyName;
        this.date = date;
        this.status = status;
    }

    // 필요하면 3개짜리(기존 호환) 생성자도 유지
    public AttendanceResponse(String className, String date, String status) {
        this(className, "", date, status);
    }

    // getter/setter
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getAcademyName() { return academyName; }
    public void setAcademyName(String academyName) { this.academyName = academyName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
