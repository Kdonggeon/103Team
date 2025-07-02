package com.mobile.greenacademypartner.model.teacher;

public class TeacherClass {


    private String classId;


    private String className;


    private String teacherId;


    private String schedule;

    // (생략 가능: Students는 지금 사용 안 하므로)

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

    public TeacherClass(String classId, String className, String teacherId, String schedule) {
        this.classId = classId;
        this.className = className;
        this.teacherId = teacherId;
        this.schedule = schedule;
    }

}
