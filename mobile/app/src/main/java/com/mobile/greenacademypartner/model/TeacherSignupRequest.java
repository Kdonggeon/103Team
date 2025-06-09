package com.mobile.greenacademypartner.model;

public class TeacherSignupRequest {
    private String teacherName;
    private String teacherId;
    private String teacherPw;
    private long teacherPhoneNumber;
    private int academyNumber;

    public TeacherSignupRequest(String teacherName, String teacherId, String teacherPw, long teacherPhoneNumber, int academyNumber) {
        this.teacherName = teacherName;
        this.teacherId = teacherId;
        this.teacherPw = teacherPw;
        this.teacherPhoneNumber = teacherPhoneNumber;
        this.academyNumber = academyNumber;
    }

    public String getTeacherPw() { return teacherPw; }
}
