// src/main/java/com/team103/dto/StudentLiteDto.java
package com.team103.dto;

import com.team103.model.Student;

import java.util.List;

public class StudentLiteDto {
    private String studentId;
    private String name;                 // Student.studentName
    private String school;
    private Integer grade;
    private String phone;                // Student.studentPhoneNumber
    private String gender;
    private List<Integer> academyNumbers;

    public StudentLiteDto() {}

    public StudentLiteDto(String studentId, String name, String school, Integer grade,
                          String phone, String gender, List<Integer> academyNumbers) {
        this.studentId = studentId;
        this.name = name;
        this.school = school;
        this.grade = grade;
        this.phone = phone;
        this.gender = gender;
        this.academyNumbers = academyNumbers;
    }

    public static StudentLiteDto from(Student s) {
        return new StudentLiteDto(
                s.getStudentId(),
                s.getStudentName(),
                s.getSchool(),
                s.getGrade(),
                s.getStudentPhoneNumber(),
                s.getGender(),
                s.getAcademyNumbers()
        );
    }

    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getSchool() { return school; }
    public Integer getGrade() { return grade; }
    public String getPhone() { return phone; }
    public String getGender() { return gender; }
    public List<Integer> getAcademyNumbers() { return academyNumbers; }

    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setName(String name) { this.name = name; }
    public void setSchool(String school) { this.school = school; }
    public void setGrade(Integer grade) { this.grade = grade; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setGender(String gender) { this.gender = gender; }
    public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }
}
