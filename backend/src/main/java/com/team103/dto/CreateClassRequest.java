package com.team103.dto;

import java.util.List;

public class CreateClassRequest {
    private String className;
    private String teacherId;
    private Integer academyNumber;

    /** 호환용(단일) */
    private Integer roomNumber;

    /** ✅ 신규: 복수 강의실 */
    private List<Integer> roomNumbers;

    // 기본 시간표(주간 반복)
    private String startTime;        // "HH:mm"
    private String endTime;          // "HH:mm"
    private List<String> daysOfWeek; // ["1","3","5"]
    private String schedule;         // (선택) "월수금 10:00"

    // getters / setters
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }

    public Integer getRoomNumber() { return roomNumber; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }

    public List<Integer> getRoomNumbers() { return roomNumbers; }
    public void setRoomNumbers(List<Integer> roomNumbers) { this.roomNumbers = roomNumbers; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public List<String> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<String> daysOfWeek) { this.daysOfWeek = daysOfWeek; }
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
}
