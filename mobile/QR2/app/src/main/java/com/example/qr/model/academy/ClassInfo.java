// src/main/java/com/example/qr/model/academy/ClassInfo.java
package com.example.qr.model.academy;

public class ClassInfo {

    private String classId;      // 강의 고유 ID
    private String className;    // 강의명
    private String teacherName;  // 담당 교사명
    private String startTime;    // 시작 시간 ("HH:mm")
    private String endTime;      // 종료 시간 ("HH:mm")
    private String dayOfWeek;    // 요일 ("월", "화", ...)

    // 기본 생성자
    public ClassInfo() {}

    // 전체 필드 생성자
    public ClassInfo(String classId, String className, String teacherName, String startTime, String endTime, String dayOfWeek) {
        this.classId = classId;
        this.className = className;
        this.teacherName = teacherName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dayOfWeek = dayOfWeek;
    }

    // Getter & Setter
    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    @Override
    public String toString() {
        return "ClassInfo{" +
                "classId='" + classId + '\'' +
                ", className='" + className + '\'' +
                ", teacherName='" + teacherName + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", dayOfWeek='" + dayOfWeek + '\'' +
                '}';
    }
}
