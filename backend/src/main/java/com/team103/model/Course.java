package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "classes")
public class Course {

    @Id
    private String id;

    @Field("Class_ID")
    private String classId;

    @Field("Class_Name")
    private String className;

    @Field("Teacher_ID")
    private String teacherId;

    @Field("Students")
    private List<String> students;

    @Field("roomNumber")
    private Integer roomNumber;

    @Field("Academy_Number")
    private Integer academyNumber;
    
    @Field("Academy_Numbers")          // 복수(있다면)
    private java.util.List<Integer> academyNumbers;

    @Field("Start_Time")   // "HH:mm"
    private String startTime;

    @Field("End_Time")     // "HH:mm"
    private String endTime;

    // 혼재될 수 있어 Object로 수용 ("１","1","월" 등)
    @Field("Days_Of_Week")
    private List<Object> daysOfWeek;

    @Field("Schedule")     // 자유 텍스트(선택)
    private String schedule;

    @Field("Extra_Dates")        // "YYYY-MM-DD"
    private List<String> extraDates;

    @Field("Cancelled_Dates")    // "YYYY-MM-DD"
    private List<String> cancelledDates;

    // ===== Getters / Setters =====
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public List<String> getStudents() { return students; }
    public void setStudents(List<String> students) { this.students = students; }

    public Integer getRoomNumber() { return roomNumber; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }

    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public List<Object> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<Object> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    public List<String> getExtraDates() { return extraDates; }
    public void setExtraDates(List<String> extraDates) { this.extraDates = extraDates; }

    public List<String> getCancelledDates() { return cancelledDates; }
    public void setCancelledDates(List<String> cancelledDates) { this.cancelledDates = cancelledDates; }
    
	public java.util.List<Integer> getAcademyNumbers() { return academyNumbers; }
	public void setAcademyNumbers(java.util.List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }
	
	public java.util.List<Integer> getAcademyNumbersSafe() {
	    if (academyNumbers != null && !academyNumbers.isEmpty()) return academyNumbers;
	    return (academyNumber != null) ? java.util.List.of(academyNumber) : java.util.List.of();
	}


    /** "１","월" 등도 1~7 정수로 변환 */
    public java.util.List<Integer> getDaysOfWeekInt() {
        if (daysOfWeek == null) return java.util.List.of();
        java.util.List<Integer> out = new java.util.ArrayList<>();
        for (Object v : daysOfWeek) {
            if (v == null) continue;
            if (v instanceof Number n) {
                int d = n.intValue(); if (1 <= d && d <= 7) out.add(d); continue;
            }
            String s = String.valueOf(v).trim();
            s = s.replace('１','1').replace('２','2').replace('３','3')
                 .replace('４','4').replace('５','5').replace('６','6')
                 .replace('７','7');
            switch (s) { case "월": out.add(1); continue; case "화": out.add(2); continue;
                case "수": out.add(3); continue; case "목": out.add(4); continue;
                case "금": out.add(5); continue; case "토": out.add(6); continue;
                case "일": out.add(7); continue; }
            try { int d = Integer.parseInt(s); if (1 <= d && d <= 7) out.add(d); } catch (Exception ignore) {}
        }
        return out;
    }

    // 편의: 토글 유틸
    public void toggleExtraDate(String date) {
        if (extraDates == null) extraDates = new java.util.ArrayList<>();
        if (extraDates.contains(date)) extraDates.remove(date); else extraDates.add(date);
    }
    public void toggleCancelledDate(String date) {
        if (cancelledDates == null) cancelledDates = new java.util.ArrayList<>();
        if (cancelledDates.contains(date)) cancelledDates.remove(date); else cancelledDates.add(date);
    }


    
}
