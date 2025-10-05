package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "attendances") // ✅ 컨트롤러/레포지토리와 컬렉션명 통일
public class Attendance {

    @Id
    private String id; // ObjectId 문자열

    @Field("Class_ID")
    private String classId;

    @Field("Date")
    private String date; // "yyyy-MM-dd"

    @Field("Session_Start")
    private String sessionStart; // "HH:mm" (옵션)

    @Field("Session_End")
    private String sessionEnd;   // "HH:mm" (옵션)

    @Field("Attendance_List")
    private List<Item> attendanceList; // ✅ 강타입 리스트

    // --- getters/setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSessionStart() { return sessionStart; }
    public void setSessionStart(String sessionStart) { this.sessionStart = sessionStart; }

    public String getSessionEnd() { return sessionEnd; }
    public void setSessionEnd(String sessionEnd) { this.sessionEnd = sessionEnd; }

    public List<Item> getAttendanceList() { return attendanceList; }
    public void setAttendanceList(List<Item> attendanceList) { this.attendanceList = attendanceList; }

    // ── 출석 엔트리 ─────────────────────────────────────────────
    public static class Item {

        @Field("Student_ID")
        private String studentId;

        @Field("Status")           // "출석"/"지각"/"결석"/"조퇴"/"미기록" 등
        private String status;

        @Field("CheckIn_Time")     // "HH:mm:ss" (옵션)
        private String checkInTime;

        @Field("Source")           // "app", "qr", "admin" 등 (옵션)
        private String source;

        // getters/setters
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCheckInTime() { return checkInTime; }
        public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }
}
