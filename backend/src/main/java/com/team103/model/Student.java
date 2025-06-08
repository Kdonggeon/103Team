package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "students")
public class Student {

    @Id
    private String id;

    @Field("Student_Name")
    private String studentName;

    @Field("Student_ID")
    private long studentId;

    @Field("Student_PW")
    private String studentPw;  // ✅ int → String 으로 변경

    @Field("Student_Address")
    private String address;

    @Field("Student_Phone_Number")
    private String phoneNumber;

    @Field("School")
    private String school;

    @Field("Grade")
    private int grade;

    @Field("Parents_Number")
    private long parentsNumber;

    @Field("Seat_Number")
    private int seatNumber;

    @Field("Checked_In")
    private boolean checkedIn;

    @Field("Gender")
    private String gender;

    public Student() {}

    public Student(String id, String studentName, long studentId, String studentPw, String address,
                   String phoneNumber, String school, int grade, long parentsNumber,
                   int seatNumber, boolean checkedIn, String gender) {
        this.id = id;
        this.studentName = studentName;
        this.studentId = studentId;
        this.studentPw = studentPw;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.school = school;
        this.grade = grade;
        this.parentsNumber = parentsNumber;
        this.seatNumber = seatNumber;
        this.checkedIn = checkedIn;
        this.gender = gender;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }

    public String getStudentPw() { return studentPw; }  // ✅ String getter
    public void setStudentPw(String studentPw) { this.studentPw = studentPw; }  // ✅ String setter

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }

    public long getParentsNumber() { return parentsNumber; }
    public void setParentsNumber(long parentsNumber) { this.parentsNumber = parentsNumber; }

    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }

    public boolean isCheckedIn() { return checkedIn; }
    public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}
