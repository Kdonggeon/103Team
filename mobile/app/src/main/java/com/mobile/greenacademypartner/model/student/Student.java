package com.mobile.greenacademypartner.model.student;

import com.google.gson.annotations.SerializedName;

public class Student {

    @SerializedName("_id")
    private String _id;

    @SerializedName("studentName")  // ✅ 대문자 아님
    private String studentName;

    @SerializedName("studentId")
    private String studentId;

    @SerializedName("Student_PW")
    private String studentPw; // ✅ int → String (암호화 문자열 받기 위해)

    @SerializedName("Student_Address")
    private String studentAddress;

    @SerializedName("Student_Phone_Number")
    private String studentPhoneNumber; // ✅ long → String (앞자리 0 보존 위해 문자열이 적합)

    @SerializedName("School")
    private String school;

    @SerializedName("Grade")
    private int grade;

    @SerializedName("Parents_Number")
    private int parentsNumber;

    @SerializedName("Seat_Number")
    private int seatNumber;

    @SerializedName("Checked_In")
    private boolean checkedIn;

    @SerializedName("Gender")
    private String gender;

    // ✅ Getter & Setter
    public String get_id() { return _id; }
    public void set_id(String _id) { this._id = _id; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentPw() { return studentPw; }
    public void setStudentPw(String studentPw) { this.studentPw = studentPw; }

    public String getStudentAddress() { return studentAddress; }
    public void setStudentAddress(String studentAddress) { this.studentAddress = studentAddress; }

    public String getStudentPhoneNumber() { return studentPhoneNumber; }
    public void setStudentPhoneNumber(String studentPhoneNumber) { this.studentPhoneNumber = studentPhoneNumber; }

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
