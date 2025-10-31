package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "rooms")
@CompoundIndexes({
    @CompoundIndex(name = "academy_room_unique",
            def = "{'Academy_Number': 1, 'Room_Number': 1}", unique = true)
})
public class Room {

    @Id
    private String id; // MongoDB ObjectId(hex) 문자열

    @Field("Room_Number")
    private int roomNumber;

    @Field("Academy_Number")
    private int academyNumber;

    // ── (레거시) 그리드 기반 ─────────────────────────────────
    private Integer rows;                 // nullable
    private Integer cols;
    private List<SeatCell> layout;

    @Field("Current_Class")
    private CurrentClass currentClass;

    @Field("Seats")
    private List<Seat> seats;

    // ── (신규) 벡터 기반 ───────────────────────────────────
    private Integer vectorVersion;        // e.g., 1
    private Double vectorCanvasW;         // 권장 1.0
    private Double vectorCanvasH;         // 권장 1.0
    private List<VectorSeat> vectorLayout;

    // ── 내부 타입들 ─────────────────────────────────────────
    public static class SeatCell {
        private Integer seatNumber;  private Integer row; private Integer col; private Boolean disabled;
        public Integer getSeatNumber() { return seatNumber; }
        public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
        public Integer getRow() { return row; }
        public void setRow(Integer row) { this.row = row; }
        public Integer getCol() { return col; }
        public void setCol(Integer col) { this.col = col; }
        public Boolean getDisabled() { return disabled; }
        public void setDisabled(Boolean disabled) { this.disabled = disabled; }
    }

    public static class CurrentClass {
        @Field("Class_ID")   private String classId;
        @Field("Class_Name") private String className;
        @Field("Teacher_ID") private String teacherId;
        @Field("Start_Time") private String startTime;
        @Field("End_Time")   private String endTime;
        public String getClassId() { return classId; }
        public void setClassId(String classId) { this.classId = classId; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public String getTeacherId() { return teacherId; }
        public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }

    public static class Seat {
        @Field("Seat_Number") private int seatNumber;
        @Field("Checked_In")  private boolean checkedIn;
        @Field("Student_ID")  private String studentId;
        public int getSeatNumber() { return seatNumber; }
        public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }
        public boolean isCheckedIn() { return checkedIn; }
        public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
    }

    public static class VectorSeat {
        private String id;         // 영구 식별자 (uuid 등)
        private String label;      // 화면 표기용
        private Double x, y, w, h; // 0..canvas 범위 (비율 권장: 0..1)
        private Double r;          // 회전 (deg) nullable
        private Boolean disabled;  // 통로 등
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
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
    }

    // ── Getters / Setters ───────────────────────────────────
    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public int getRoomNumber() { return roomNumber; } public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }
    public int getAcademyNumber() { return academyNumber; } public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }
    public Integer getRows() { return rows; } public void setRows(Integer rows) { this.rows = rows; }
    public Integer getCols() { return cols; } public void setCols(Integer cols) { this.cols = cols; }
    public List<SeatCell> getLayout() { return layout; } public void setLayout(List<SeatCell> layout) { this.layout = layout; }
    public CurrentClass getCurrentClass() { return currentClass; } public void setCurrentClass(CurrentClass currentClass) { this.currentClass = currentClass; }
    public List<Seat> getSeats() { return seats; } public void setSeats(List<Seat> seats) { this.seats = seats; }
    public Integer getVectorVersion() { return vectorVersion; } public void setVectorVersion(Integer vectorVersion) { this.vectorVersion = vectorVersion; }
    public Double getVectorCanvasW() { return vectorCanvasW; } public void setVectorCanvasW(Double vectorCanvasW) { this.vectorCanvasW = vectorCanvasW; }
    public Double getVectorCanvasH() { return vectorCanvasH; } public void setVectorCanvasH(Double vectorCanvasH) { this.vectorCanvasH = vectorCanvasH; }
    public List<VectorSeat> getVectorLayout() { return vectorLayout; } public void setVectorLayout(List<VectorSeat> vectorLayout) { this.vectorLayout = vectorLayout; }
}
