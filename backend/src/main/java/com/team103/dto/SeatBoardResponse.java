package com.team103.dto;

import java.util.List;

public class SeatBoardResponse {

    private CurrentClass currentClass; // null 가능
    private String date;               // "yyyy-MM-dd"
    private String layoutType;         // "grid" | "vector" (선택, 기본 "grid")
    private int rows;                  // grid 전용
    private int cols;                  // grid 전용
    private List<SeatStatus> seats;

    // --- getters/setters ---
    public CurrentClass getCurrentClass() { return currentClass; }
    public void setCurrentClass(CurrentClass currentClass) { this.currentClass = currentClass; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getLayoutType() { return layoutType; }
    public void setLayoutType(String layoutType) { this.layoutType = layoutType; }
    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }
    public int getCols() { return cols; }
    public void setCols(int cols) { this.cols = cols; }
    public List<SeatStatus> getSeats() { return seats; }
    public void setSeats(List<SeatStatus> seats) { this.seats = seats; }

    // ── nested ─────────────────────────────────────────────

    public static class CurrentClass {
        private String classId;
        private String className;
        public String getClassId() { return classId; }
        public void setClassId(String classId) { this.classId = classId; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
    }

    public static class SeatStatus {
        private Integer seatNumber;     // label
        private Integer row;            // grid 좌표(선택)
        private Integer col;            // grid 좌표(선택)
        private boolean disabled;
        private String studentId;       // 배정 학생
        private String attendanceStatus;// PRESENT/LATE/ABSENT 등
        private String occupiedAt;      // ISO-8601(선택)

        public Integer getSeatNumber() { return seatNumber; }
        public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
        public Integer getRow() { return row; }
        public void setRow(Integer row) { this.row = row; }
        public Integer getCol() { return col; }
        public void setCol(Integer col) { this.col = col; }
        public boolean isDisabled() { return disabled; }
        public void setDisabled(boolean disabled) { this.disabled = disabled; }
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getAttendanceStatus() { return attendanceStatus; }
        public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }
        public String getOccupiedAt() { return occupiedAt; }
        public void setOccupiedAt(String occupiedAt) { this.occupiedAt = occupiedAt; }
    }
}
