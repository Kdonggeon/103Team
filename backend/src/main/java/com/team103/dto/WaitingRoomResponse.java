package com.team103.dto;



  //대기실 목록을 조회할 때 서버가 프론트엔드로 전달
  //waiting_room 컬렉션의 데이터와 students 컬렉션의 학생 정보를 결합해 전송
 
public class WaitingRoomResponse {

    /** 학생 고유 ID */
    private String studentId;

    /** 학생 이름 */
    private String studentName;

    /** 학생 학교 */
    private String school;

    /** 학생 학년 */
    private int grade;

    /** 학원 번호 */
    private int academyNumber;

    /** 등원 시각 (QR 찍은 시각) */
    private String checkedInAt;

    /** 학생의 현재 상태 "LOBBY", "SEATED" 같은 학생이 대기실에 있거나 수업을 듣거나 하는등의 코드  */
    private String status;

    // --- Getter & Setter ---
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }

    public String getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(String checkedInAt) { this.checkedInAt = checkedInAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
