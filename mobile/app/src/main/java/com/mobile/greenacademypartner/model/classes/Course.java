package com.mobile.greenacademypartner.model.classes;

public class Course {
    private String classId;
    private String className;
    private String teacherId;
    private String schedule;

    // 생성자 (필요에 따라 추가 가능)
    public Course() {}

    // Getter & Setter
    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }
}
