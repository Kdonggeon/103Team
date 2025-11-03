package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "attendances")
// Class_ID + Date로 빠른 조회 (하루 1문서 전략)
@CompoundIndex(name = "class_date_idx", def = "{'Class_ID': 1, 'Date': 1}", unique = true)
public class Attendance {

    @Id
    private String id; // ObjectId 문자열

    @Field("Class_ID")
    private String classId;

    @Field("Date")
    private String date; // "yyyy-MM-dd"

    @Field("Session_Start") // 옵션
    private String sessionStart; // "HH:mm"

    @Field("Session_End")   // 옵션
    private String sessionEnd;   // "HH:mm"

    @Field("Attendance_List")
    private List<Item> attendanceList;

    /** ✅ 날짜별 좌석 배정: 수업별-날짜별로 저장 */
    @Field("Seat_Assignments")
    private List<SeatAssign> seatAssignments;

    // ── getters/setters ─────────────────────────────────────────────
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

    public List<SeatAssign> getSeatAssignments() { return seatAssignments; }
    public void setSeatAssignments(List<SeatAssign> seatAssignments) { this.seatAssignments = seatAssignments; }

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

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCheckInTime() { return checkInTime; }
        public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }

    // ── 좌석 배정 엔트리 ───────────────────────────────────────────
    public static class SeatAssign {
        @Field("Seat_Id")    private String seatId;     // VectorSeat.id (옵션)
        @Field("Seat_Label") private String seatLabel;  // "1","A-3" 등 (옵션)
        @Field("Student_ID") private String studentId;

        public String getSeatId() { return seatId; }
        public void setSeatId(String seatId) { this.seatId = seatId; }

        public String getSeatLabel() { return seatLabel; }
        public void setSeatLabel(String seatLabel) { this.seatLabel = seatLabel; }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
    }
}
