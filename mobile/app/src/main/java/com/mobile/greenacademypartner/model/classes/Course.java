package com.mobile.greenacademypartner.model.classes;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Course {

    @SerializedName(value = "classId", alternate = {"Class_ID", "id"})
    private String classId;

    // 🔥 서버가 내려주는 실제 필드명: name
    @SerializedName(value = "name", alternate = {"Name"})
    private String name;

    // 이전 구조(className)도 남겨둠
    @SerializedName(value = "className", alternate = {"Class_Name"})
    private String className;

    @SerializedName(value = "teacherId", alternate = {"Teacher_ID"})
    private String teacherId;

    @SerializedName(value = "students", alternate = {"Students"})
    private List<String> students;

    @SerializedName(value = "daysOfWeek", alternate = {"Days_Of_Week"})
    private List<Integer> daysOfWeek;

    @SerializedName(value = "startTime", alternate = {"Start_Time"})
    private String startTime;

    @SerializedName(value = "endTime", alternate = {"End_Time"})
    private String endTime;

    @SerializedName(value = "schedule", alternate = {"Schedule"})
    private String schedule;

    @SerializedName("todayStatus")
    private String todayStatus;

    // 🔥 수업 진행 상태 (예정 / 진행중 / 종료)
    private String status;

    // 🔥 학원 이름 추가 (백엔드에서 내려오는 academyName 사용)
    @SerializedName(value = "academyName", alternate = {"Academy_Name"})
    private String academyName;

    //────────────────────────────────────
    // GETTER / SETTER
    //────────────────────────────────────

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTodayStatus() { return todayStatus; }
    public void setTodayStatus(String todayStatus) { this.todayStatus = todayStatus; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    // 🔥 최우선 사용: name
    public String getName() {
        if (name != null && !name.trim().isEmpty()) return name;
        return className; // fallback
    }

    public void setName(String name) { this.name = name; }

    public String getClassName() {
        if (className != null && !className.trim().isEmpty()) return className;
        return name; // fallback
    }

    public void setClassName(String className) { this.className = className; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public List<String> getStudents() { return students; }
    public void setStudents(List<String> students) { this.students = students; }

    public List<Integer> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<Integer> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    // 🔥 학원 이름 getter/setter
    public String getAcademyName() {
        return academyName != null ? academyName : "";
    }

    public void setAcademyName(String academyName) {
        this.academyName = academyName;
    }
}
