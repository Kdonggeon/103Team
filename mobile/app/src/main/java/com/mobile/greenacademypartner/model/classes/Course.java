package com.mobile.greenacademypartner.model.classes;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Course {

    @SerializedName(value = "classId",   alternate = {"Class_ID"})
    private String classId;

    @SerializedName(value = "className", alternate = {"Class_Name"})
    private String className;

    @SerializedName(value = "teacherId", alternate = {"Teacher_ID"})
    private String teacherId;

    @SerializedName(value = "students",  alternate = {"Students"})
    private List<String> students;

    // 요일/시간
    @SerializedName(value = "daysOfWeek", alternate = {"Days_Of_Week"})
    private List<Integer> daysOfWeek;  // 1=월 … 7=일

    @SerializedName(value = "startTime",  alternate = {"Start_Time"})
    private String startTime;           // "HH:mm"

    @SerializedName(value = "endTime",    alternate = {"End_Time"})
    private String endTime;             // "HH:mm"

    @SerializedName(value = "schedule",   alternate = {"Schedule"})
    private String schedule;

    // ----- getters / setters -----
    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getClassName() { return className; }
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
}
