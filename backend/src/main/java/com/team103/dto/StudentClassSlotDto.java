package com.team103.dto;

public class StudentClassSlotDto {

    /** 수업 ID (Class_ID 또는 _id) */
    private String classId;

    /** 수업 이름 (Class_Name) */
    private String className;

    /** 수업 날짜 (YYYY-MM-DD) */
    private String date;

    /** 요일 (1=월 … 7=일, java.time.DayOfWeek 값과 동일) */
    private Integer dayOfWeek;

    /** 강의실 번호 (날짜별 오버라이드 우선) */
    private Integer roomNumber;

    /** 시작 시간 "HH:mm" */
    private String startTime;

    /** 종료 시간 "HH:mm" */
    private String endTime;

    /** (선택) 학원 번호 */
    private Integer academyNumber;

    // ===== Getter / Setter =====

    public String getClassId() {
        return classId;
    }
    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }
    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Integer getRoomNumber() {
        return roomNumber;
    }
    public void setRoomNumber(Integer roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getStartTime() {
        return startTime;
    }
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getAcademyNumber() {
        return academyNumber;
    }
    public void setAcademyNumber(Integer academyNumber) {
        this.academyNumber = academyNumber;
    }
}
