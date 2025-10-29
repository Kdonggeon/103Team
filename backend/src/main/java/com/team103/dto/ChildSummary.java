package com.team103.dto;

import java.util.List;

public class ChildSummary {
    private String studentId;
    private String studentName;
    private List<Integer> academies;

    public ChildSummary() {}

    public ChildSummary(String studentId, String studentName, List<Integer> academies) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.academies = academies;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public List<Integer> getAcademies() { return academies; }
    public void setAcademies(List<Integer> academies) { this.academies = academies; }
}
