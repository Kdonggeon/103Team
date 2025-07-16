package com.team103.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team103.model.Student;

public class StudentSignupRequest {
    private String id;
    @JsonProperty("Student_ID")
    private String studentId;

    @JsonProperty("Student_PW")
    private String studentPw;

    @JsonProperty("Student_Name")
    private String studentName;

    @JsonProperty("Student_Address")
    private String address;

    @JsonProperty("Student_Phone_Number")
    private String phoneNumber;

    @JsonProperty("School")
    private String school;

    @JsonProperty("Grade")
    private int grade;

    @JsonProperty("Parents_Number")
    private String parentsNumber;

    @JsonProperty("Seat_Number")
    private int seatNumber;

    @JsonProperty("Checked_In")
    private boolean checkedIn;

    @JsonProperty("Gender")
    private String gender;

    public Student toEntity() {
        return new Student(
                id,
                null,           
                studentName,
                studentId,
                studentPw,
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
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentPw() { return studentPw; }
    public void setStudentPw(String studentPw) { this.studentPw = studentPw; }
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
