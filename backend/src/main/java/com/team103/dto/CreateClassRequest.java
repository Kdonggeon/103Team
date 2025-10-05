package com.team103.dto;

public class CreateClassRequest {
    private String className;
    private String teacherId;
    private Integer academyNumber;
    private Integer roomNumber; // optional

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }
    public Integer getRoomNumber() { return roomNumber; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }
}
