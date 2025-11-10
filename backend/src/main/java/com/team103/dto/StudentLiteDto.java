// src/main/java/com/team103/dto/StudentLiteDto.java
package com.team103.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.team103.model.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentLiteDto {

    private String studentId;
    private String name;                 // Student.studentName
    private String school;
    private Integer grade;
    private String phone;                // Student.studentPhoneNumber
    private String gender;
    private List<Integer> academyNumbers = new ArrayList<>();

    public StudentLiteDto() {
        // no-args
    }

    public StudentLiteDto(String studentId,
                          String name,
                          String school,
                          Integer grade,
                          String phone,
                          String gender,
                          List<Integer> academyNumbers) {
        this.studentId = studentId;
        this.name = name;
        this.school = school;
        this.grade = grade;
        this.phone = phone;
        this.gender = gender;
        setAcademyNumbers(academyNumbers); // null 가드
    }

    public static StudentLiteDto from(Student s) {
        if (s == null) return new StudentLiteDto();
        return new StudentLiteDto(
                s.getStudentId(),
                s.getStudentName(),
                s.getSchool(),
                s.getGrade(),
                s.getStudentPhoneNumber(),
                s.getGender(),
                s.getAcademyNumbers() != null ? s.getAcademyNumbers() : List.of()
        );
    }

    // --- getters ---
    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getSchool() { return school; }
    public Integer getGrade() { return grade; }
    public String getPhone() { return phone; }
    public String getGender() { return gender; }

    /**
     * academyNumbers는 항상 null이 아닌 리스트를 반환
     */
    public List<Integer> getAcademyNumbers() {
        if (academyNumbers == null) {
            academyNumbers = new ArrayList<>();
        }
        return academyNumbers;
    }

    // --- setters ---
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setName(String name) { this.name = name; }
    public void setSchool(String school) { this.school = school; }
    public void setGrade(Integer grade) { this.grade = grade; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setGender(String gender) { this.gender = gender; }

    public void setAcademyNumbers(List<Integer> academyNumbers) {
        // null 방지
        this.academyNumbers = (academyNumbers == null) ? new ArrayList<>() : new ArrayList<>(academyNumbers);
    }
}
