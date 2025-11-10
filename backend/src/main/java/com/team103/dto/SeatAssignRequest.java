// src/main/java/com/team103/dto/SeatAssignRequest.java
package com.team103.dto;

/**
 * 좌석 QR 스캔 / 교사 수동 배정 공통 요청 DTO
 * - classId가 없으면 (academyNumber, roomNumber, now)로 현재 수업을 서버가 판정
 */
public class SeatAssignRequest {

    /** 학생의 고유 ID (필수) */
    private String studentId;

    /** 학원 번호 (필수) */
    private int academyNumber;

    /** 강의실 번호 (필수) */
    private int roomNumber;

    /** 좌석 번호 (필수) */
    private int seatNumber;

    /** 선택: 수업 지정. 없으면 서버가 현재 진행중 수업을 판정 */
    private String classId;

    /** 선택: "yyyy-MM-dd" (없으면 today) */
    private String date;

    /** 선택: "qr" | "manual" 등 */
    private String source;

    /** 선택: true면 좌석 배정 해제 */
    private Boolean unassign;

    // --- getters/setters ---
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Boolean getUnassign() { return unassign; }
    public void setUnassign(Boolean unassign) { this.unassign = unassign; }
}
