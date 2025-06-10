package com.team103.dto;

public class TeacherUpdateRequest {
    private String teacherId;
    private String teacherName;
    private String teacherPhoneNumber;
    private int academyNumber;

    public TeacherUpdateRequest() {}

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getTeacherPhoneNumber() { return teacherPhoneNumber; }
    public void setTeacherPhoneNumber(String teacherPhoneNumber) { this.teacherPhoneNumber = teacherPhoneNumber; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }
}
