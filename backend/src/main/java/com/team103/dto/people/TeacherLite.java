package com.team103.dto.people;

public class TeacherLite {
    private String teacherId;
    private String name;
    private String phone;
    private Integer academyNumber;

    public TeacherLite() {}
    public TeacherLite(String teacherId, String name, String phone, Integer academyNumber) {
        this.teacherId = teacherId; this.name = name; this.phone = phone; this.academyNumber = academyNumber;
    }
    public String getTeacherId() { return teacherId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public Integer getAcademyNumber() { return academyNumber; }
}
