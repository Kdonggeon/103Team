// src/main/java/com/team103/dto/ClassLite.java
package com.team103.dto.people;

import java.util.List;

public class ClassLite {
    private String classId;
    private String className;
    private List<String> dayOfWeek; // ["MON","WED"] 등
    private String startTime;       // "13:00"
    private String endTime;         // "15:00"
    private Integer roomNumber;     // 방 번호

    public ClassLite() {}
    public ClassLite(String classId, String className, List<String> dayOfWeek,
                     String startTime, String endTime, Integer roomNumber) {
        this.classId = classId;
        this.className = className;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.roomNumber = roomNumber;
    }

    public String getClassId() { return classId; }
    public String getClassName() { return className; }
    public List<String> getDayOfWeek() { return dayOfWeek; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public Integer getRoomNumber() { return roomNumber; }

    public void setClassId(String v){ this.classId=v; }
    public void setClassName(String v){ this.className=v; }
    public void setDayOfWeek(List<String> v){ this.dayOfWeek=v; }
    public void setStartTime(String v){ this.startTime=v; }
    public void setEndTime(String v){ this.endTime=v; }
    public void setRoomNumber(Integer v){ this.roomNumber=v; }
}
