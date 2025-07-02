package com.mobile.greenacademypartner.model.teacher;

public class TeacherSignupRequest {
    private String teacherName;
    private String teacherId;
    private String teacherPw;
    private String teacherPhoneNumber;
    private int academyNumber;

    public TeacherSignupRequest(String teacherName, String teacherId, String teacherPw, String teacherPhoneNumber, int academyNumber) {
        this.teacherName = teacherName;
        this.teacherId = teacherId;
        this.teacherPw = teacherPw;
        this.teacherPhoneNumber = teacherPhoneNumber;
        this.academyNumber = academyNumber;
    }

    public String getTeacherPw() { return teacherPw; }
}
