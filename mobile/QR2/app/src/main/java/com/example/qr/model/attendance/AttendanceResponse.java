package com.example.qr.model.attendance;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AttendanceResponse {
    @SerializedName("academyName")
    private String academyName;

    @SerializedName("className")
    private String className;

    @SerializedName("date")       // "yyyy-MM-dd"
    private String date;

    @SerializedName("status")     // "출석"/"지각"/"결석" 등
    private String status;

    @SerializedName("startTime")  // "HH:mm"
    private String startTime;

    @SerializedName("endTime")    // "HH:mm"
    private String endTime;

    private List<Integer> daysOfWeek;

    // --- getters / setters ---

    public String getAcademyName() { return academyName; }
    public void setAcademyName(String academyName) { this.academyName = academyName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public List<Integer> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<Integer> daysOfWeek) { this.daysOfWeek = daysOfWeek; }
}
