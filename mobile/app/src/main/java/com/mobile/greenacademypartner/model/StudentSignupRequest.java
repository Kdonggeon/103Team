package com.mobile.greenacademypartner.model;

import com.google.gson.annotations.SerializedName;

public class StudentSignupRequest {
    @SerializedName("studentId")
    private int studentId;

    @SerializedName("studentPw")
    private int studentPw;

    @SerializedName("studentName")
    private String studentName;

    @SerializedName("studentPhone")
    private String studentPhone;

    @SerializedName("studentBirth")
    private String studentBirth;

    @SerializedName("studentGender")
    private String studentGender;

    public StudentSignupRequest(int studentId, int studentPw, String studentName,
                                String studentPhone, String studentBirth, String studentGender) {
        this.studentId = studentId;
        this.studentPw = studentPw;
        this.studentName = studentName;
        this.studentPhone = studentPhone;
        this.studentBirth = studentBirth;
        this.studentGender = studentGender;
    }
}
