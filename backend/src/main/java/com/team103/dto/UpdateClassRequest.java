package com.team103.dto;

public class UpdateClassRequest {
    private String className;
    private Integer roomNumber;     // optional
    private Integer academyNumber;  // optional

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public Integer getRoomNumber() { return roomNumber; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }
    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }
}
