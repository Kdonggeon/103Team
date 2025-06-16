package com.mobile.greenacademypartner.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TeacherAttendance {

    // ✅ @SerializedName 제거
    private String classId;
    private String date;
    private List<AttendanceRecord> attendanceList;


    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<AttendanceRecord> getAttendanceList() {
        return attendanceList;
    }

    public void setAttendanceList(List<AttendanceRecord> attendanceList) {
        this.attendanceList = attendanceList;
    }

    public static class AttendanceRecord {
        private String studentId;
        private String status;

        public String getStudentId() {
            return studentId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
