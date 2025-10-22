// src/main/java/com/team103/dto/ScheduleItem.java
package com.team103.dto;

public class ScheduleItem {
    private String scheduleId;   // classId@YYYY-MM-DD
    private String teacherId;
    private String date;         // YYYY-MM-DD
    private String classId;
    private String title;        // 보통 반 이름
    private String startTime;    // HH:mm
    private String endTime;      // HH:mm
    private Integer roomNumber;  // 선택
    private String memo;         // 선택

    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Integer getRoomNumber() { return roomNumber; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
