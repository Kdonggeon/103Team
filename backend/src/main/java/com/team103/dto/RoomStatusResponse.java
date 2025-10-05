package com.team103.dto;

import java.util.List;


  //강의실의 현재 수업(CurrentClass) 정보와
  //좌석 배열(Seats)의 점유 상태를 함께 반환하기 위한 응답 구조.
 
public class RoomStatusResponse {

    /** 학원 번호 */
    private int academyNumber;

    /** 강의실 번호 */
    private int roomNumber;

    /** 현재 수업 정보 */
    private CurrentClass currentClass;

    /** 좌석 배열 정보 */
    private List<SeatStatus> seats;

    // --- 내부 클래스: CurrentClass ---
    public static class CurrentClass {
        private String classId;
        private String className;
        private String teacherId;
        private String startTime;
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

    // --- 내부 클래스: SeatStatus ---
    public static class SeatStatus {
        private int seatNumber;
        private String studentId;
        private String studentName;
        private boolean occupied;
        private String occupiedAt;

        // --- Getter & Setter ---
        public int getSeatNumber() { return seatNumber; }
        public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }

        public boolean isOccupied() { return occupied; }
        public void setOccupied(boolean occupied) { this.occupied = occupied; }

        public String getOccupiedAt() { return occupiedAt; }
        public void setOccupiedAt(String occupiedAt) { this.occupiedAt = occupiedAt; }
    }

    // --- Getter & Setter ---
    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public CurrentClass getCurrentClass() { return currentClass; }
    public void setCurrentClass(CurrentClass currentClass) { this.currentClass = currentClass; }

    public List<SeatStatus> getSeats() { return seats; }
    public void setSeats(List<SeatStatus> seats) { this.seats = seats; }
}
