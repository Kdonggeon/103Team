package com.team103.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.team103.model.Student;

public class StudentSignupRequest {

    private String id;

    @JsonProperty("Student_ID")           @JsonAlias("studentId")
    private String studentId;

    @JsonProperty("Student_PW")           @JsonAlias("studentPw")
    private String studentPw;

    @JsonProperty("Student_Name")         @JsonAlias("studentName")
    private String studentName;

    @JsonProperty("Student_Address")      @JsonAlias("address")
    private String address;

    @JsonProperty("Student_Phone_Number") @JsonAlias({"phoneNumber","studentPhoneNumber"})
    private String phoneNumber;

    @JsonProperty("School")               @JsonAlias("school")
    private String school;

    @JsonProperty("Grade")                @JsonAlias("grade")
    private int grade; // primitive → 미전송 시 0

    @JsonProperty("Parents_Number")       @JsonAlias("parentsNumber")
    private String parentsNumber;

    @JsonProperty("Seat_Number")          @JsonAlias("seatNumber")
    private int seatNumber; // 미전송 시 0

    @JsonProperty("Checked_In")           @JsonAlias("checkedIn")
    private boolean checkedIn; // 미전송 시 false

    @JsonProperty("Gender")               @JsonAlias("gender")
    private String gender;

    /** 암호화된 비밀번호를 받아 엔티티 생성 */
    public Student toEntity(String encodedPw) {
        return new Student(
            id,
            null,             // 생성 시점에 불필요한 필드이면 null
            studentName,
            studentId,
            encodedPw,        // ← 암호화된 비밀번호 주입
            address,
            phoneNumber,
            school,
            grade,
            parentsNumber,
            seatNumber,
            checkedIn,
            gender
        );
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentPw() { return studentPw; }
    public void setStudentPw(String studentPw) { this.studentPw = studentPw; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }

    public String getParentsNumber() { return parentsNumber; }
    public void setParentsNumber(String parentsNumber) { this.parentsNumber = parentsNumber; }

    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }

    public boolean isCheckedIn() { return checkedIn; }
    public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}
