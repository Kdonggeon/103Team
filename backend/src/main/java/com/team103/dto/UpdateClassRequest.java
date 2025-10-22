package com.team103.dto;

import java.util.List;

public class UpdateClassRequest {
    private String className;
    private Integer roomNumber;
    private Integer academyNumber;

    private String startTime;        // "HH:mm"
    private String endTime;          // "HH:mm"
    private List<String> daysOfWeek; // ["1","3","5"]
    private String schedule;         // 자유 텍스트(선택)

    // getters / setters
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public Integer getRoomNumber() { return roomNumber; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }
    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public List<String> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<String> daysOfWeek) { this.daysOfWeek = daysOfWeek; }
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
}
