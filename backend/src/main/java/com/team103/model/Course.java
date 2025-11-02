// src/main/java/com/team103/model/Course.java
package com.team103.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** 
     * ✅ DB에는 숫자/문자 혼재 가능 → 원본은 Object로 받는다.
     *    JSON(프론트)에는 항상 문자열 배열로 내려준다.
     */
    @Field("Students")
    @JsonIgnore
    private List<Object> studentsRaw;

    /** 호환용(단일) */
    @Field("roomNumber")
    private Integer roomNumber;

    /** ✅ 복수 강의실 */
    @Field("Room_Numbers")
    private List<Integer> roomNumbers;

    @Field("Academy_Number")
    private Integer academyNumber;

    @Field("Academy_Numbers")
    private List<Integer> academyNumbers;

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

    /* ✅ 추가: 날짜별 시간/강의실 오버라이드 */
    @Field("Date_Time_Overrides")   // { "2025-10-27": { "start":"10:00", "end":"11:00" }, ... }
    private Map<String, DailyTime> dateTimeOverrides;

    @Field("Date_Room_Overrides")   // { "2025-10-27": 403, ... }
    private Map<String, Integer> dateRoomOverrides;

    /* ===== Getters / Setters ===== */
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    /** ✅ 프론트로는 항상 문자열 배열 */
    @JsonProperty("students")
    public List<String> getStudents() {
        if (studentsRaw == null) return null;
        List<String> out = new ArrayList<>(studentsRaw.size());
        for (Object v : studentsRaw) out.add(v == null ? null : String.valueOf(v));
        return out;
    }

    /** ✅ 프론트에서 올 때는 문자열 배열을 그대로 저장(원하면 숫자 변환 로직 추가 가능) */
    @JsonProperty("students")
    public void setStudents(List<String> students) {
        if (students == null) {
            this.studentsRaw = null;
        } else {
            this.studentsRaw = new ArrayList<>(students);
        }
    }

    public Integer getRoomNumber() { return roomNumber; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }

    public List<Integer> getRoomNumbers() { return roomNumbers; }
    public void setRoomNumbers(List<Integer> roomNumbers) { this.roomNumbers = roomNumbers; }

    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }

    public List<Integer> getAcademyNumbers() { return academyNumbers; }
    public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }

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

    public Map<String, DailyTime> getDateTimeOverrides() { return dateTimeOverrides; }
    public void setDateTimeOverrides(Map<String, DailyTime> dateTimeOverrides) { this.dateTimeOverrides = dateTimeOverrides; }

    public Map<String, Integer> getDateRoomOverrides() { return dateRoomOverrides; }
    public void setDateRoomOverrides(Map<String, Integer> dateRoomOverrides) { this.dateRoomOverrides = dateRoomOverrides; }

    /* ===== 편의 유틸 ===== */

    /** academyNumbers 비어있을 때 단일 필드로 보정 */
    public List<Integer> getAcademyNumbersSafe() {
        if (academyNumbers != null && !academyNumbers.isEmpty()) return academyNumbers;
        return (academyNumber != null) ? List.of(academyNumber) : List.of();
    }

    /** ✅ 스케줄/표시 등에 쓸 1순위 강의실 번호 (roomNumbers 우선, 없으면 roomNumber) */
    public Integer getPrimaryRoomNumber() {
        if (roomNumbers != null && !roomNumbers.isEmpty()) return roomNumbers.get(0);
        return roomNumber;
    }

    /** "１","월" 등도 1~7 정수로 변환 */
    public List<Integer> getDaysOfWeekInt() {
        if (daysOfWeek == null) return List.of();
        List<Integer> out = new ArrayList<>();
        for (Object v : daysOfWeek) {
            if (v == null) continue;
            if (v instanceof Number n) {
                int d = n.intValue(); if (1 <= d && d <= 7) out.add(d); continue;
            }
            String s = String.valueOf(v).trim();
            s = s.replace('１','1').replace('２','2').replace('３','3')
                 .replace('４','4').replace('５','5').replace('６','6')
                 .replace('７','7');
            switch (s) {
                case "월": out.add(1); continue; case "화": out.add(2); continue;
                case "수": out.add(3); continue; case "목": out.add(4); continue;
                case "금": out.add(5); continue; case "토": out.add(6); continue;
                case "일": out.add(7); continue;
            }
            try { int d = Integer.parseInt(s); if (1 <= d && d <= 7) out.add(d); } catch (Exception ignore) {}
        }
        return out;
    }

    public void toggleExtraDate(String date) {
        if (extraDates == null) extraDates = new ArrayList<>();
        if (extraDates.contains(date)) extraDates.remove(date); else extraDates.add(date);
    }
    public void toggleCancelledDate(String date) {
        if (cancelledDates == null) cancelledDates = new ArrayList<>();
        if (cancelledDates.contains(date)) cancelledDates.remove(date); else cancelledDates.add(date);
    }

    /* ===== 날짜별 오버라이드 접근 ===== */

    public DailyTime getTimeFor(String dateYmd) {
        if (dateTimeOverrides != null && dateTimeOverrides.containsKey(dateYmd)) {
            return dateTimeOverrides.get(dateYmd);
        }
        return new DailyTime(nullSafe(startTime), nullSafe(endTime));
    }

    public Integer getRoomFor(String dateYmd) {
        if (dateRoomOverrides != null && dateRoomOverrides.containsKey(dateYmd)) {
            return dateRoomOverrides.get(dateYmd);
        }
        return getPrimaryRoomNumber();
    }

    public void putOverride(String dateYmd, String start, String end, Integer room) {
        if (dateTimeOverrides == null) dateTimeOverrides = new LinkedHashMap<>();
        dateTimeOverrides.put(dateYmd, new DailyTime(start, end));
        if (room != null) {
            if (dateRoomOverrides == null) dateRoomOverrides = new LinkedHashMap<>();
            dateRoomOverrides.put(dateYmd, room);
        }
    }

    public void clearOverride(String dateYmd) {
        if (dateTimeOverrides != null) dateTimeOverrides.remove(dateYmd);
        if (dateRoomOverrides != null) dateRoomOverrides.remove(dateYmd);
    }

    private static String nullSafe(String s) { return (s == null || s.isBlank()) ? null : s; }

    /* ===== 내장 타입 ===== */
    public static class DailyTime {
        @Field("start")
        private String start;  // "HH:mm"
        @Field("end")
        private String end;    // "HH:mm"
        public DailyTime() {}
        public DailyTime(String start, String end) { this.start = start; this.end = end; }
        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }
        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }
}
