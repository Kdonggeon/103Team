package com.team103.model;

import java.util.Collections;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "teachers")
public class Teacher {

	private String fcmToken;
    @Id
    private String id;

    @Field("Teacher_Name")
    private String teacherName;

    @Field("Teacher_ID")
    private String teacherId;

    @Field("Teacher_PW")
    private String teacherPw;

    @Field("Teacher_Phone_Number")
    private String teacherPhoneNumber;

    @Field("Academy_Number")
    private List<Integer> academyNumbers;

    public Teacher(String id, String teacherName, String teacherId, String teacherPw,
                   String teacherPhoneNumber, List<Integer> academyNumbers) {
        this.id = id;
        this.teacherName = teacherName;
        this.teacherId = teacherId;
        this.teacherPw = teacherPw;
        this.teacherPhoneNumber = teacherPhoneNumber;
        this.academyNumbers = academyNumbers;
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

    public List<Integer> getAcademyNumbers() { return academyNumbers; }
    public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}
