package com.mobile.greenacademypartner.model.student;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Student {

    // MongoDB/ObjectId 등
    @SerializedName(value = "_id", alternate = {"id"})
    private String _id;

    @SerializedName(value = "studentName", alternate = {"Student_Name", "name"})
    private String studentName;

    @SerializedName(value = "studentId", alternate = {"Student_ID", "studentNumber"})
    private String studentId;

    @SerializedName(value = "studentPw", alternate = {"Student_PW"})
    private String studentPw;

    @SerializedName(value = "studentAddress", alternate = {"Student_Address", "address"})
    private String studentAddress;

    @SerializedName(value = "studentPhoneNumber", alternate = {"Student_Phone_Number", "phone", "tel"})
    private String studentPhoneNumber;

    @SerializedName(value = "school", alternate = {"School", "schoolName"})
    private String school;

    @SerializedName(value = "grade", alternate = {"Grade"})
    private int grade;

    @SerializedName(value = "parentsNumber", alternate = {"Parents_Number", "parentNumber"})
    private int parentsNumber;

    @SerializedName(value = "seatNumber", alternate = {"Seat_Number"})
    private int seatNumber;

    @SerializedName(value = "checkedIn", alternate = {"Checked_In"})
    private boolean checkedIn;

    @SerializedName(value = "gender", alternate = {"Gender", "sex"})
    private String gender;

    // 서버/DB에 따라 Academy_Numbers / Academy_Number / academyNumbers 등 섞일 수 있어 모두 허용
    @SerializedName(value = "academyNumbers", alternate = {"Academy_Numbers", "Academy_Number"})
    private List<Integer> Academy_Numbers;

    // ===== Getter & Setter =====
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

    public List<Integer> getAcademy_Numbers() { return Academy_Numbers; }
    public void setAcademy_Numbers(List<Integer> academy_Numbers) { this.Academy_Numbers = academy_Numbers; }
}
