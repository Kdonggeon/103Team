package com.mobile.greenacademypartner.model;

import com.google.gson.annotations.SerializedName;

public class StudentSignupRequest {

    @SerializedName("studentId")
    private String studentId;

    @SerializedName("studentPw")
    private String studentPw;

    @SerializedName("studentName")
    private String studentName;

    @SerializedName("studentPhone")
    private String studentPhone;

    @SerializedName("studentBirth")
    private String studentBirth;

    @SerializedName("studentGender")
    private String studentGender;

    public StudentSignupRequest(String studentId, String studentPw, String studentName,
                                String studentPhone, String studentBirth, String studentGender) {
        this.studentId = studentId;
        this.studentPw = studentPw;
        this.studentName = studentName;
        this.studentPhone = studentPhone;
        this.studentBirth = studentBirth;
        this.studentGender = studentGender;
    }

    // Getter 예시 (response.body() 활용 시 필요)
    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }
}
