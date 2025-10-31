// src/main/java/com/team103/model/Room.java
package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "rooms")
@CompoundIndexes({
    @CompoundIndex(name = "academy_room_unique", def = "{'Academy_Number': 1, 'Room_Number': 1}", unique = true)
})
public class Room {

    @Id
    private String id;  // ✅ MongoDB ObjectId 자동 생성

    @Field("Room_Number")
    private int roomNumber;

    @Field("Academy_Number")
    private int academyNumber;

    // ====== 벡터 버전/캔버스 크기(DB 그대로) ======
    @Field("vectorVersion")
    private Integer vectorVersion;

    @Field("vectorCanvasW")
    private Integer vectorCanvasW;

    @Field("vectorCanvasH")
    private Integer vectorCanvasH;

    // ====== 기존 필드 유지 ======
    private Integer rows;
    private Integer cols;

    // ✅ DB의 vectorLayout을 여기에 매핑
    @Field("vectorLayout")
    private List<SeatCell> layout;

    @Field("Current_Class")
    private CurrentClass currentClass;  // 현재 진행 중인 수업 정보

    @Field("Seats")
    private List<Seat> seats;  // (기존 구조 유지: null일 수 있음)

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }

    public Integer getVectorVersion() { return vectorVersion; }
    public void setVectorVersion(Integer vectorVersion) { this.vectorVersion = vectorVersion; }

    public Integer getVectorCanvasW() { return vectorCanvasW; }
    public void setVectorCanvasW(Integer vectorCanvasW) { this.vectorCanvasW = vectorCanvasW; }

    public Integer getVectorCanvasH() { return vectorCanvasH; }
    public void setVectorCanvasH(Integer vectorCanvasH) { this.vectorCanvasH = vectorCanvasH; }

    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }

    public Integer getCols() { return cols; }
    public void setCols(Integer cols) { this.cols = cols; }

    // 기존 이름(layout)으로 접근
    public List<SeatCell> getLayout() { return layout; }
    public void setLayout(List<SeatCell> layout) { this.layout = layout; }

    public CurrentClass getCurrentClass() { return currentClass; }
    public void setCurrentClass(CurrentClass currentClass) { this.currentClass = currentClass; }

    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }

    // ====== 💡 alias: 컨트롤러/프론트에서 vectorLayout 이름으로도 접근 가능 ======
    public List<SeatCell> getVectorLayout() { return layout; }
    public void setVectorLayout(List<SeatCell> v) { this.layout = v; }

    // (선택) rows/cols가 비어있을 때 vectorCanvas를 폴백으로 쓸 수 있게 헬퍼
    public Integer getEffectiveRows() { return rows != null ? rows : vectorCanvasH; }
    public Integer getEffectiveCols() { return cols != null ? cols : vectorCanvasW; }

    // --- 내부 클래스 ---
    public static class SeatCell {
        private Integer seatNumber;
        private Integer row;
        private Integer col;
        private Boolean disabled;

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
}
