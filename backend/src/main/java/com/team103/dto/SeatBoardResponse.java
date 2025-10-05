package com.team103.dto;

import com.team103.model.Room;
import java.util.List;

public class SeatBoardResponse {
    private Room.CurrentClass currentClass;
    private Integer rows;
    private Integer cols;
    private List<SeatStatus> seats;

    public Room.CurrentClass getCurrentClass() { return currentClass; }
    public void setCurrentClass(Room.CurrentClass currentClass) { this.currentClass = currentClass; }
    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }
    public Integer getCols() { return cols; }
    public void setCols(Integer cols) { this.cols = cols; }
    public List<SeatStatus> getSeats() { return seats; }
    public void setSeats(List<SeatStatus> seats) { this.seats = seats; }

    // 각 좌석 칸의 상태
    public static class SeatStatus {
        private Integer seatNumber;
        private Integer row;
        private Integer col;
        private Boolean disabled;
        private String studentId;
        private String attendanceStatus; // 출석/지각/결석/미기록

        public Integer getSeatNumber() { return seatNumber; }
        public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
        public Integer getRow() { return row; }
        public void setRow(Integer row) { this.row = row; }
        public Integer getCol() { return col; }
        public void setCol(Integer col) { this.col = col; }
        public Boolean getDisabled() { return disabled; }
        public void setDisabled(Boolean disabled) { this.disabled = disabled; }
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getAttendanceStatus() { return attendanceStatus; }
        public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    }
}
