package com.team103.dto.people;

public class StudentLite {
    private String studentId;
    private String name;
    private String school;
    private Integer grade;

    public StudentLite() {}
    public StudentLite(String studentId, String name, String school, Integer grade) {
        this.studentId = studentId; this.name = name; this.school = school; this.grade = grade;
    }
    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getSchool() { return school; }
    public Integer getGrade() { return grade; }
}
