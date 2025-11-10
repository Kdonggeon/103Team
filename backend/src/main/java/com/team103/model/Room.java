// src/main/java/com/team103/model/Room.java
package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Document(collection = "rooms")
@CompoundIndexes({
    @CompoundIndex(
        name = "academy_room_unique",
        def = "{'Academy_Number': 1, 'Room_Number': 1}",
        unique = true
    )
})
public class Room {

    @Id
    private String id; // MongoDB ObjectId(hex) 문자열

    @Field("Room_Number")
    private int roomNumber;

    @Field("Academy_Number")
    private int academyNumber;

    /* ================= 레거시 그리드 (DB 비저장) ================= */
    @Transient
    private Integer rows;

    @Transient
    private Integer cols;

    /** 과거 코드 호환용: grid 레이아웃(저장 안 함) */
    @Transient
    private List<SeatCell> legacyGridLayout;

    /* ================= 현재 수업/기존 Seats ================= */
    @Field("Current_Class")
    private CurrentClass currentClass;    // 현재 진행 중인 수업 정보

    @Field("Seats")
    private List<Seat> seats;             // (기존 구조 유지: null일 수 있음)

    /* ================= 벡터 기반 자유 배치 (DB 저장) ================= */
    @Field("vectorVersion")
    private Integer vectorVersion;        // e.g., 1

    @Field("vectorCanvasW")
    private Double vectorCanvasW;         // 권장 1.0

    @Field("vectorCanvasH")
    private Double vectorCanvasH;         // 권장 1.0

    /** ✅ 실제 사용: VectorSeat를 DB의 "vectorLayout"에 저장/로드 (layout2의 단일 소스) */
    @Field("vectorLayout")
    private List<VectorSeat> vectorLayout;

    /* ================= 내부 타입들 ================= */
    /** 레거시 그리드용(저장 안함) */
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

    /** ✅ VectorSeat: VectorSeatEditor/rooms.vector.ts와 1:1 매핑 */
    public static class VectorSeat {
        private String id;         // 영구 식별자 (uuid 등)
        private String label;      // 좌석 라벨(표기)
        private Double x, y, w, h; // 0..canvas 범위(비율 권장: 0..1)
        private Double r;          // 회전 (deg) nullable
        private Boolean disabled;  // 통로/비활성 등

        @Field("Student_ID")
        private String studentId;  // QR 체크인/배정 시 저장

        @Field("occupiedAt")
        private Instant occupiedAt; // 선택(점유 시각)

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
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public Instant getOccupiedAt() { return occupiedAt; }
        public void setOccupiedAt(Instant occupiedAt) { this.occupiedAt = occupiedAt; }
    }

    /* ================= Getters / Setters ================= */
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }

    public CurrentClass getCurrentClass() { return currentClass; }
    public void setCurrentClass(CurrentClass currentClass) { this.currentClass = currentClass; }

    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }

    public Integer getVectorVersion() { return vectorVersion; }
    public void setVectorVersion(Integer vectorVersion) { this.vectorVersion = vectorVersion; }

    public Double getVectorCanvasW() { return vectorCanvasW; }
    public void setVectorCanvasW(Double vectorCanvasW) { this.vectorCanvasW = vectorCanvasW; }

    public Double getVectorCanvasH() { return vectorCanvasH; }
    public void setVectorCanvasH(Double vectorCanvasH) { this.vectorCanvasH = vectorCanvasH; }

    /** 자유 배치 레이아웃(실제 DB 저장 필드; layout2) */
    public List<VectorSeat> getVectorLayout() { return vectorLayout; }
    public void setVectorLayout(List<VectorSeat> vectorLayout) { this.vectorLayout = vectorLayout; }

    /* ---- 과거 코드/서비스 호환용 alias 메서드들 ---- */

    /** 일부 서비스가 리플렉션으로 부르는 getVectorLayoutV2() 대응 → 동일 데이터 반환 */
    public List<VectorSeat> getVectorLayoutV2() { return getVectorLayout(); }

    /** (grid) 예전 코드가 room.getLayout()을 호출하는 경우가 있어 alias 제공 */
    public List<SeatCell> getLayout() { return legacyGridLayout; }
    public void setLayout(List<SeatCell> layout) { this.legacyGridLayout = layout; }

    // 레거시 그리드 (비저장 필드 접근자)
    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }
    public Integer getCols() { return cols; }
    public void setCols(Integer cols) { this.cols = cols; }
    public List<SeatCell> getLegacyGridLayout() { return legacyGridLayout; }
    public void setLegacyGridLayout(List<SeatCell> legacyGridLayout) { this.legacyGridLayout = legacyGridLayout; }
}
