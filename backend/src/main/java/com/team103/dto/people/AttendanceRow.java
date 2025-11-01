// src/main/java/com/team103/dto/AttendanceRow.java
package com.team103.dto.people;

public class AttendanceRow {
    private String date;      // "2025-10-31"
    private String classId;
    private String className; // 보강해서 내려줌
    private String status;    // "PRESENT" | "LATE" | "ABSENT"

    public AttendanceRow() {}
    public AttendanceRow(String date, String classId, String className, String status) {
        this.date = date; this.classId = classId; this.className = className; this.status = status;
    }

    public String getDate() { return date; }
    public String getClassId() { return classId; }
    public String getClassName() { return className; }
    public String getStatus() { return status; }

    public void setDate(String v){ this.date=v; }
    public void setClassId(String v){ this.classId=v; }
    public void setClassName(String v){ this.className=v; }
    public void setStatus(String v){ this.status=v; }
}
