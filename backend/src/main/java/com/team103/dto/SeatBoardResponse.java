// src/main/java/com/team103/dto/SeatBoardResponse.java
package com.team103.dto;

import java.util.List;

public class SeatBoardResponse {

    private CurrentClass currentClass;
    private String date;

    /** "grid" | "vector" */
    private String layoutType;

    /** grid 모드용 (nullable로 두어 0 직렬화 방지) */
    private Integer rows;
    private Integer cols;

    /** vector 모드용 (정규화 캔버스 크기, 보통 1.0/1.0) */
    private Double canvasW;
    private Double canvasH;

    private List<SeatStatus> seats;
    private List<WaitingItem> waiting;

    /** 카운트도 null 허용(없으면 프론트 계산) */
    private Integer presentCount;
    private Integer lateCount;
    private Integer absentCount;
    private Integer moveOrBreakCount;
    private Integer notRecordedCount;

    // ── Getters / Setters ─────────────────────────────
    public CurrentClass getCurrentClass() { return currentClass; }
    public void setCurrentClass(CurrentClass currentClass) { this.currentClass = currentClass; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLayoutType() { return layoutType; }
    public void setLayoutType(String layoutType) { this.layoutType = layoutType; }

    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }

    public Integer getCols() { return cols; }
    public void setCols(Integer cols) { this.cols = cols; }

    public Double getCanvasW() { return canvasW; }
    public void setCanvasW(Double canvasW) { this.canvasW = canvasW; }

    public Double getCanvasH() { return canvasH; }
    public void setCanvasH(Double canvasH) { this.canvasH = canvasH; }

    public List<SeatStatus> getSeats() { return seats; }
    public void setSeats(List<SeatStatus> seats) { this.seats = seats; }

    public List<WaitingItem> getWaiting() { return waiting; }
    public void setWaiting(List<WaitingItem> waiting) { this.waiting = waiting; }

    public Integer getPresentCount() { return presentCount; }
    public void setPresentCount(Integer presentCount) { this.presentCount = presentCount; }

    public Integer getLateCount() { return lateCount; }
    public void setLateCount(Integer lateCount) { this.lateCount = lateCount; }

    public Integer getAbsentCount() { return absentCount; }
    public void setAbsentCount(Integer absentCount) { this.absentCount = absentCount; }

    public Integer getMoveOrBreakCount() { return moveOrBreakCount; }
    public void setMoveOrBreakCount(Integer moveOrBreakCount) { this.moveOrBreakCount = moveOrBreakCount; }

    public Integer getNotRecordedCount() { return notRecordedCount; }
    public void setNotRecordedCount(Integer notRecordedCount) { this.notRecordedCount = notRecordedCount; }

    // ── Nested Classes ─────────────────────────────
    public static class CurrentClass {
        private String classId;
        private String className;
        public String getClassId() { return classId; }
        public void setClassId(String classId) { this.classId = classId; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
    }

    public static class SeatStatus {
        /** grid 모드 */
        private Integer seatNumber;
        private Integer row;
        private Integer col;

        /** vector 모드 좌표(0~1) */
        private Double x, y, w, h, r;

        private Boolean disabled;
        private String studentId;
        private String studentName;
        private String attendanceStatus;
        private String occupiedAt;

        public Integer getSeatNumber() { return seatNumber; }
        public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
        public Integer getRow() { return row; }
        public void setRow(Integer row) { this.row = row; }
        public Integer getCol() { return col; }
        public void setCol(Integer col) { this.col = col; }

        public Double getX() { return x; }
        public void setX(Double x) { this.x = x; }
        public Double getY() { return y; }
        public void setY(Double y) { this.y = y; }
        public Double getW() { return w; }
        public void setW(Double w) { this.w = w; }
        public Double getH() { return h; }
        public void setH(Double h) { this.h = h; }
        public Double getR() { return r; }
        public void setR(Double r) { this.r = r; }

        public Boolean getDisabled() { return disabled; }
        public void setDisabled(Boolean disabled) { this.disabled = disabled; }
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public String getAttendanceStatus() { return attendanceStatus; }
        public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }
        public String getOccupiedAt() { return occupiedAt; }
        public void setOccupiedAt(String occupiedAt) { this.occupiedAt = occupiedAt; }
    }

    public static class WaitingItem {
        private String studentId;
        private String studentName;
        private String status;
        private String checkedInAt;

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCheckedInAt() { return checkedInAt; }
        public void setCheckedInAt(String checkedInAt) { this.checkedInAt = checkedInAt; }
    }
}
