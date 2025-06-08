package com.team103.dto;

import com.team103.model.Student;

public class StudentSignupRequest {

    private String id;
    private String studentName;
    private long studentId;
    private String studentPw; // ✅ 암호화 전 문자열 비밀번호
    private String address;
    private String phoneNumber;
    private String school;
    private int grade;
    private long parentsNumber;
    private int seatNumber;
    private boolean checkedIn;
    private String gender;

    // 🔄 DTO → Entity 변환 메서드
    public Student toEntity() {
        return new Student(
                id,
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

    // ✅ Getter & Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }

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

    public long getParentsNumber() { return parentsNumber; }
    public void setParentsNumber(long parentsNumber) { this.parentsNumber = parentsNumber; }

    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }

    public boolean isCheckedIn() { return checkedIn; }
    public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}
