package com.team103.dto;

import java.util.List;

public class StudentClassLiteDto {

    // 기존 필드
    private String id;
    private String name;

    // ✅ 시간표용으로 추가할 필드들
    private Integer roomNumber;       // 강의실 번호
    private String startTime;         // "HH:mm"
    private String endTime;           // "HH:mm"
    private List<Integer> daysOfWeek; // 1~7 (월=1 … 일=7)

    public StudentClassLiteDto() {
    }

    // ✅ 기존에 쓰고 있던 2-파라미터 생성자 유지
    public StudentClassLiteDto(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // ✅ 필요하면 시간표까지 한 번에 채우는 생성자 추가 (선택)
    public StudentClassLiteDto(
            String id,
            String name,
            Integer roomNumber,
            String startTime,
            String endTime,
            List<Integer> daysOfWeek
    ) {
        this.id = id;
        this.name = name;
        this.roomNumber = roomNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.daysOfWeek = daysOfWeek;
    }

    // ----- 기존 getter/setter -----
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    // ----- 새 필드 getter/setter -----
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

    public List<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }
    public void setDaysOfWeek(List<Integer> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }
}
