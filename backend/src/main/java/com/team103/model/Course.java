// src/main/java/com/team103/model/Course.java
package com.team103.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.*;

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
     * 🔥 Students (MongoDB 그대로)
     *  - ["0000","abcdefg"] 같은 문자열 배열
     */
    @Field("Students")
    private List<String> students;

    /** 단일 roomNumber 호환 */
    @Field("roomNumber")
    private Integer roomNumber;

    /** 복수 강의실 */
    @Field("Room_Numbers")
    private List<Integer> roomNumbers;

    @Field("Academy_Number")
    private Integer academyNumber;

    @Field("Academy_Numbers")
    private List<Integer> academyNumbers;

    @Field("Start_Time") // "HH:mm"
    private String startTime;

    @Field("End_Time")   // "HH:mm"
    private String endTime;

    // 요일 정보 (문자/숫자 혼재 수용)
    @Field("Days_Of_Week")
    private List<Object> daysOfWeek;

    @Field("Schedule")
    private String schedule;

    @Field("Extra_Dates")
    private List<String> extraDates;

    @Field("Cancelled_Dates")
    private List<String> cancelledDates;

    /* 날짜별 시간 오버라이드 */
    @Field("Date_Time_Overrides")
    private Map<String, DailyTime> dateTimeOverrides;

    /* 날짜별 강의실 오버라이드 */
    @Field("Date_Room_Overrides")
    private Map<String, Integer> dateRoomOverrides;

    /* 좌석 배정 맵 (roomNumber -> (seatLabel -> studentId)) */
    @Field("Seat_Map")
    private Map<Integer, Map<String, String>> seatMap;


    /* =========================
       기본 Getter/Setter
       ========================= */

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    /** 🔥 studentsRaw 제거 → students 그대로 사용 */
    @JsonProperty("students")
    public List<String> getStudents() {
        return students;
    }

    @JsonProperty("students")
    public void setStudents(List<String> students) {
        this.students = students;
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

    public Map<Integer, Map<String, String>> getSeatMap() { return seatMap; }
    public void setSeatMap(Map<Integer, Map<String, String>> seatMap) { this.seatMap = seatMap; }



    /* =========================
       편의 유틸
       ========================= */

    /** academyNumbers 비어있으면 단일 필드로 보정 */
    public List<Integer> getAcademyNumbersSafe() {
        if (academyNumbers != null && !academyNumbers.isEmpty())
            return academyNumbers;
        return (academyNumber != null) ? List.of(academyNumber) : List.of();
    }

    /** 기본 강의실 */
    public Integer getPrimaryRoomNumber() {
        if (roomNumbers != null && !roomNumbers.isEmpty())
            return roomNumbers.get(0);
        return roomNumber;
    }

    /** 요일 정수 리스트 변환 */
    public List<Integer> getDaysOfWeekInt() {
        if (daysOfWeek == null) return List.of();
        List<Integer> out = new ArrayList<>();
        for (Object v : daysOfWeek) {
            if (v == null) continue;

            if (v instanceof Number n) {
                int d = n.intValue();
                if (1 <= d && d <= 7) out.add(d);
                continue;
            }

            String s = String.valueOf(v).trim();
            s = s.replace('１','1').replace('２','2').replace('３','3')
                 .replace('４','4').replace('５','5').replace('６','6')
                 .replace('７','7');

            switch (s) {
                case "월" -> out.add(1);
                case "화" -> out.add(2);
                case "수" -> out.add(3);
                case "목" -> out.add(4);
                case "금" -> out.add(5);
                case "토" -> out.add(6);
                case "일" -> out.add(7);
                default -> {
                    try {
                        int d = Integer.parseInt(s);
                        if (1 <= d && d <= 7) out.add(d);
                    } catch (Exception ignore) {}
                }
            }
        }
        return out;
    }

    /** 날짜별 시간 가져오기 */
    public DailyTime getTimeFor(String dateYmd) {
        if (dateTimeOverrides != null && dateTimeOverrides.containsKey(dateYmd))
            return dateTimeOverrides.get(dateYmd);
        return new DailyTime(nullSafe(startTime), nullSafe(endTime));
    }

    /** 날짜별 강의실 가져오기 */
    public Integer getRoomFor(String dateYmd) {
        if (dateRoomOverrides != null && dateRoomOverrides.containsKey(dateYmd))
            return dateRoomOverrides.get(dateYmd);
        return getPrimaryRoomNumber();
    }

    /** 오버라이드 적용 */
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


    /* =========================
       좌석 배정 유틸
       ========================= */

    public String getAssignedStudentId(Integer roomNumber, String label){
        if (seatMap == null || roomNumber == null || label == null) return null;
        Map<String,String> m = seatMap.get(roomNumber);
        return (m == null) ? null : m.get(label);
    }

    /** 동일 학생 중복 배정 방지 */
    public void assignSeat(Integer roomNumber, String label, String studentId){
        if (roomNumber == null || label == null) return;

        seatMap = (seatMap == null) ? new HashMap<>() : seatMap;
        seatMap.computeIfAbsent(roomNumber, k -> new HashMap<>());
        Map<String,String> m = seatMap.get(roomNumber);

        // 기존 좌석 해제
        m.entrySet().removeIf(e -> studentId != null && studentId.equals(e.getValue()));

        // 새 배정
        if (studentId == null || studentId.isBlank()) {
            m.remove(label);
        } else {
            m.put(label, studentId);
        }
    }

    public void clearSeatStudent(Integer roomNumber, String studentId){
        if (seatMap == null || roomNumber == null || studentId == null) return;
        Map<String,String> m = seatMap.get(roomNumber);
        if (m == null) return;
        m.entrySet().removeIf(e -> studentId.equals(e.getValue()));
    }


    /* =========================
       내장 클래스 - DailyTime
       ========================= */

    public static class DailyTime {
        @Field("start")
        private String start;

        @Field("end")
        private String end;

        public DailyTime() {}
        public DailyTime(String start, String end) { this.start = start; this.end = end; }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }
    
    /* ===== 날짜 토글 복원 ===== */

    public void toggleExtraDate(String date) {
        if (extraDates == null) extraDates = new ArrayList<>();
        if (extraDates.contains(date)) {
            extraDates.remove(date);
        } else {
            extraDates.add(date);
        }
    }

    public void toggleCancelledDate(String date) {
        if (cancelledDates == null) cancelledDates = new ArrayList<>();
        if (cancelledDates.contains(date)) {
            cancelledDates.remove(date);
        } else {
            cancelledDates.add(date);
        }
    }

}
