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

    private Integer rows;
    private Integer cols;
    private List<SeatCell> layout;

    @Field("Current_Class")
    private CurrentClass currentClass;

    @Field("Seats")
    private List<Seat> seats;

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }

    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }

    public Integer getCols() { return cols; }
    public void setCols(Integer cols) { this.cols = cols; }

    public List<SeatCell> getLayout() { return layout; }
    public void setLayout(List<SeatCell> layout) { this.layout = layout; }

    public CurrentClass getCurrentClass() { return currentClass; }
    public void setCurrentClass(CurrentClass currentClass) { this.currentClass = currentClass; }

    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }

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
