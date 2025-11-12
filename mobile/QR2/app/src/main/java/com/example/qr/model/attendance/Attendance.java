package com.example.qr.model.attendance;

import java.util.List;

public class Attendance {

    private String classId;
    private String date;
    private List<AttendanceEntry> attendanceList;
    private String className;

    public String getClassId() {
        return classId;
    }

    public String getDate() {
        return date;
    }

    public List<AttendanceEntry> getAttendanceList() {
        return attendanceList;
    }

    public String getClassName() {
        return className != null ? className : classId;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
