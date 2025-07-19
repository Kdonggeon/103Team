package com.team103.dto;

import java.util.Collections;

import com.team103.model.Teacher;

public class TeacherSignupRequest {

    private String id;
    private String teacherName;
    private String teacherId;
    private String teacherPw;
    private String teacherPhoneNumber;
    private int academyNumber;

    public Teacher toEntity(String encodedPw) {
        return new Teacher(id, teacherName, teacherId, encodedPw, teacherPhoneNumber,  Collections.singletonList(academyNumber));
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getTeacherPw() { return teacherPw; }
    public void setTeacherPw(String teacherPw) { this.teacherPw = teacherPw; }

    public String getTeacherPhoneNumber() { return teacherPhoneNumber; }
    public void setTeacherPhoneNumber(String teacherPhoneNumber) { this.teacherPhoneNumber = teacherPhoneNumber; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }
}
