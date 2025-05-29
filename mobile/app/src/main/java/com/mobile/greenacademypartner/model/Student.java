package com.mobile.greenacademypartner.model;

public class Student {
    private int _id;
    private String studentName;
    private long studentId;
    private int studentPw;
    private String studentAddress;
    private long studentPhoneNumber;
    private String school;
    private int grade;
    private int parentsNumber;
    private int seatNumber;
    private boolean checkedIn;
    private String gender;

    // Getter/Setter
    public int get_id() { return _id; }
    public void set_id(int _id) { this._id = _id; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }

    public int getStudentPw() { return studentPw; }
    public void setStudentPw(int studentPw) { this.studentPw = studentPw; }

    public String getStudentAddress() { return studentAddress; }
    public void setStudentAddress(String studentAddress) { this.studentAddress = studentAddress; }

    public long getStudentPhoneNumber() { return studentPhoneNumber; }
    public void setStudentPhoneNumber(long studentPhoneNumber) { this.studentPhoneNumber = studentPhoneNumber; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }

    public int getParentsNumber() { return parentsNumber; }
    public void setParentsNumber(int parentsNumber) { this.parentsNumber = parentsNumber; }

    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }

    public boolean isCheckedIn() { return checkedIn; }
    public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}
