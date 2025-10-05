package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

@Document(collection = "rooms")
public class Room {

    @Id
    private int id;

    @Field("Room_Number")
    private int roomNumber;

    @Field("Academy_Number")
    private int academyNumber;

    @Field("Current_Class")
    private CurrentClass currentClass;  // 현재 진행 중인 수업 정보

    @Field("Seats")
    private List<Seat> seats;  // 좌석 배열

    // --- Getter & Setter ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }

    public CurrentClass getCurrentClass() { return currentClass; }
    public void setCurrentClass(CurrentClass currentClass) { this.currentClass = currentClass; }

    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }

    // --- 내부 클래스: CurrentClass ---
    public static class CurrentClass {
        @Field("Class_ID")
        private String classId;

        @Field("Class_Name")
        private String className;

        @Field("Teacher_ID")
        private String teacherId;

        @Field("Start_Time")
        private String startTime;

        @Field("End_Time")
        private String endTime;

        // --- Getter & Setter ---
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

 // --- 내부 클래스: Seat ---
    public static class Seat {

        @Field("Seat_Number")
        private int seatNumber;

        @Field("Student_ID")
        private String studentId;  // 해당 좌석의 학생 ID

        @Field("Checked_In")
        private boolean checkedIn; // 출석 여부 (기존 기능용)

        @Field("Occupied")
        private boolean occupied;  // 좌석 사용 여부 (QR 기반 좌석 배정용)

        @Field("Occupied_At")
        private String occupiedAt; // 착석 시각 (ISO-8601)

        // --- Getter & Setter ---
        public int getSeatNumber() {
            return seatNumber;
        }

        public void setSeatNumber(int seatNumber) {
            this.seatNumber = seatNumber;
        }

        public String getStudentId() {
            return studentId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }

        public boolean isCheckedIn() {
            return checkedIn;
        }

        public void setCheckedIn(boolean checkedIn) {
            this.checkedIn = checkedIn;
        }

        public boolean isOccupied() {
            return occupied;
        }

        public void setOccupied(boolean occupied) {
            this.occupied = occupied;
        }

        public String getOccupiedAt() {
            return occupiedAt;
        }

        public void setOccupiedAt(String occupiedAt) {
            this.occupiedAt = occupiedAt;
        }
    }
}
