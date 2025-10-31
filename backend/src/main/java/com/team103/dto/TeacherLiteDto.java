// src/main/java/com/team103/dto/TeacherLiteDto.java
package com.team103.dto;

import com.team103.model.Teacher;

import java.util.List;

public class TeacherLiteDto {
    private String teacherId;
    private String name;                 // Teacher.teacherName
    private String phone;                // Teacher.teacherPhoneNumber
    private List<Integer> academyNumbers;

    public TeacherLiteDto() {}

    public TeacherLiteDto(String teacherId, String name, String phone, List<Integer> academyNumbers) {
        this.teacherId = teacherId;
        this.name = name;
        this.phone = phone;
        this.academyNumbers = academyNumbers;
    }

    public static TeacherLiteDto from(Teacher t) {
        return new TeacherLiteDto(
                t.getTeacherId(),
                t.getTeacherName(),
                t.getTeacherPhoneNumber(),
                t.getAcademyNumbers()
        );
    }

    public String getTeacherId() { return teacherId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public List<Integer> getAcademyNumbers() { return academyNumbers; }

    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }
}
