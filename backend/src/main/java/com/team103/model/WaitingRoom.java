package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Document(collection = "waiting_room")
public class WaitingRoom {

    @Id
    private String id; // MongoDB ObjectId

    @Field("Student_ID")
    private String studentId; // students.Student_ID 참조

    @Field("Academy_Number")
    private int academyNumber; // 학원 번호

    @Field("Checked_In_At")
    private String checkedInAt; // 입구 QR 찍은 시각 (ISO 8601 형식)

    @Field("Status")
    private String status; // 대기 상태 ("LOBBY" 고정값)

    // --- 기본 생성자 ---
    public WaitingRoom() {}

    // --- 전체 필드 생성자 ---
    public WaitingRoom(String studentId, int academyNumber, String checkedInAt, String status) {
        this.studentId = studentId;
        this.academyNumber = academyNumber;
        this.checkedInAt = checkedInAt;
        this.status = status;
    }

    // --- Getter & Setter ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public int getAcademyNumber() {
        return academyNumber;
    }

    public void setAcademyNumber(int academyNumber) {
        this.academyNumber = academyNumber;
    }

    public String getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(String checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // --- 디버깅용 toString() ---
    @Override
    public String toString() {
        return "WaitingRoom{" +
                "id='" + id + '\'' +
                ", studentId='" + studentId + '\'' +
                ", academyNumber=" + academyNumber +
                ", checkedInAt='" + checkedInAt + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
