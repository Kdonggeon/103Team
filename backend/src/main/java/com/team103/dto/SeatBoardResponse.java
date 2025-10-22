// src/main/java/com/team103/dto/SeatBoardResponse.java
package com.team103.dto;

import java.util.List;

public class SeatBoardResponse {
    private CurrentClass currentClass; // { classId, className }
    private int rows;
    private int cols;
    private List<SeatStatus> seats;

    public CurrentClass getCurrentClass() { return currentClass; }
    public void setCurrentClass(CurrentClass currentClass) { this.currentClass = currentClass; }
    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }
    public int getCols() { return cols; }
    public void setCols(int cols) { this.cols = cols; }
    public List<SeatStatus> getSeats() { return seats; }
    public void setSeats(List<SeatStatus> seats) { this.seats = seats; }

    // ── 중첩 클래스들 ───────────────────────────────────────────────

    public static class CurrentClass {
        private String classId;
        private String className;

        public String getClassId() { return classId; }
        public void setClassId(String classId) { this.classId = classId; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
    }

    public static class SeatStatus {
        private Integer seatNumber;
        private Integer row;
        private Integer col;
        private boolean disabled;
        private String studentId;
        private String attendanceStatus;

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
    }
}
