package com.team103.dto;


 //좌석 QR 스캔 또는 교사 수동 배정 시 서버로 전달
 //어떤 학생이 어떤 강의실의 몇 번 좌석에 앉았는지를 알려줌

public class SeatAssignRequest {

    /** 학생의 고유 ID */
    private String studentId;

    /** 학원 번호 */
    private int academyNumber;

    /** 강의실 번호 */
    private int roomNumber;

    /** 좌석 번호 */
    private int seatNumber;

    // --- Getter & Setter ---
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }
}
