package com.team103.dto;

 //학원 입구 QR 스캔 시, 서버로 전달됨
 //학생이 학원에 등원할 때 필요한 최소 정보만 포함한다.
 
public class EntranceRequest {

    // 학생의 고유 ID (students.Student_ID 참조) 
    private String studentId;

    /// 학원 고유 번호 
    private int academyNumber;

    // --- Getter & Setter ---
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }
}
