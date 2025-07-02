package com.mobile.greenacademypartner.model.teacher;

public class TeacherUpdateRequest {
    private String teacherId;
    private String teacherName;
    private String teacherPhoneNumber;
    private int academyNumber;

    public TeacherUpdateRequest() {}

    public TeacherUpdateRequest(String teacherId, String teacherName, String teacherPhoneNumber, int academyNumber) {
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.teacherPhoneNumber = teacherPhoneNumber;
        this.academyNumber = academyNumber;
    }

    public String getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public String getTeacherPhoneNumber() { return teacherPhoneNumber; }
    public int getAcademyNumber() { return academyNumber; }

    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public void setTeacherPhoneNumber(String teacherPhoneNumber) { this.teacherPhoneNumber = teacherPhoneNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }
}
