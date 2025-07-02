package com.mobile.greenacademypartner.model.student;

import com.google.gson.annotations.SerializedName;

public class StudentSignupRequest {

    @SerializedName("Student_ID")
    private String studentId;

    @SerializedName("Student_PW")
    private String studentPw;

    @SerializedName("Student_Name")
    private String studentName;

    @SerializedName("Student_Address")
    private String studentAddress;

    @SerializedName("Student_Phone_Number")
    private String studentPhoneNumber;

    @SerializedName("School")
    private String school;

    @SerializedName("Grade")
    private int grade;

    @SerializedName("Parents_Number")
    private String parentsNumber;

    @SerializedName("Seat_Number")
    private int seatNumber;

    @SerializedName("Checked_In")
    private boolean checkedIn;

    @SerializedName("Gender")
    private String gender;

    public StudentSignupRequest(String studentId, String studentPw, String studentName,
                                String studentAddress, String studentPhoneNumber, String school,
                                int grade, String parentsNumber, int seatNumber, boolean checkedIn,
                                String gender) {

        if (studentPw == null || studentPw.isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 null이거나 빈 값일 수 없습니다.");
        }

        this.studentId = studentId;
        this.studentPw = studentPw;
        this.studentName = studentName;
        this.studentAddress = studentAddress;
        this.studentPhoneNumber = studentPhoneNumber;
        this.school = school;
        this.grade = grade;
        this.parentsNumber = parentsNumber;
        this.seatNumber = seatNumber;
        this.checkedIn = checkedIn;
        this.gender = gender;
    }


    // Getters (필요 시 setters도 추가)
    public String getStudentId() { return studentId; }
    public String getStudentPw() { return studentPw; }
    public String getStudentName() { return studentName; }
    public String getStudentAddress() { return studentAddress; }
    public String getStudentPhoneNumber() { return studentPhoneNumber; }
    public String getSchool() { return school; }
    public int getGrade() { return grade; }
    public  String getParentsNumber() { return parentsNumber; }
    public int getSeatNumber() { return seatNumber; }
    public boolean isCheckedIn() { return checkedIn; }
    public String getGender() { return gender; }
}
