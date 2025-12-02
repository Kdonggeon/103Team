package com.mobile.greenacademypartner.model.classes;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class Course {

    @SerializedName(value = "classId", alternate = {"Class_ID", "id"})
    private String classId;

    @SerializedName(value = "name", alternate = {"Name"})
    private String name;

    @SerializedName(value = "className", alternate = {"Class_Name"})
    private String className;

    @SerializedName(value = "teacherId", alternate = {"Teacher_ID"})
    private String teacherId;

    @SerializedName(value = "students", alternate = {"Students"})
    private List<String> students;

    // 요일 반복 수업 → 이제 안 쓸 예정이지만 남겨둠
    @SerializedName(value = "daysOfWeek", alternate = {"Days_Of_Week"})
    private List<Integer> daysOfWeek;

    @SerializedName(value = "startTime", alternate = {"Start_Time"})
    private String startTime;

    @SerializedName(value = "endTime", alternate = {"End_Time"})
    private String endTime;

    @SerializedName(value = "schedule", alternate = {"Schedule"})
    private String schedule;

    @SerializedName("todayStatus")
    private String todayStatus;

    // 학원 정보
    @SerializedName(value = "academyNumber", alternate = {"Academy_Number"})
    private Integer academyNumber;

    @SerializedName(value = "academyName", alternate = {"Academy_Name"})
    private String academyName;


    //──────────────────────────────────────────────
    // 🔥🔥 추가된 부분: 단발성 날짜 + 날짜별 시간 변경 🔥🔥
    //──────────────────────────────────────────────

    @SerializedName(value = "extraDates", alternate = {"Extra_Dates"})
    private List<String> extraDates;

    @SerializedName(value = "dateTimeOverrides", alternate = {"Date_Time_Overrides"})
    private Map<String, DailyTime> dateTimeOverrides;


    //──────────────────────────────────────────────
    // Getter / Setter
    //──────────────────────────────────────────────

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getName() {
        if (name != null && !name.isEmpty()) return name;
        return className;
    }

    public void setName(String name) { this.name = name; }

    public String getClassName() {
        if (className != null && !className.isEmpty()) return className;
        return name;
    }

    public void setClassName(String className) { this.className = className; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public List<String> getStudents() { return students; }
    public void setStudents(List<String> students) { this.students = students; }

    public List<Integer> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<Integer> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    public String getTodayStatus() { return todayStatus; }
    public void setTodayStatus(String todayStatus) { this.todayStatus = todayStatus; }

    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }

    public String getAcademyName() { return academyName; }
    public void setAcademyName(String academyName) { this.academyName = academyName; }


    //──────────────────────────────────────────────
    // 🔥 단발성 날짜 getter/setter
    //──────────────────────────────────────────────

    public List<String> getExtraDates() { return extraDates; }
    public void setExtraDates(List<String> extraDates) { this.extraDates = extraDates; }


    //──────────────────────────────────────────────
    // 🔥 날짜별 시간 오버라이드
    //──────────────────────────────────────────────

    public Map<String, DailyTime> getDateTimeOverrides() { return dateTimeOverrides; }
    public void setDateTimeOverrides(Map<String, DailyTime> dateTimeOverrides) { this.dateTimeOverrides = dateTimeOverrides; }

    public DailyTime getTimeFor(String dateYmd) {
        if (dateTimeOverrides != null && dateTimeOverrides.containsKey(dateYmd)) {
            return dateTimeOverrides.get(dateYmd);
        }
        return new DailyTime(startTime, endTime);
    }


    //──────────────────────────────────────────────
    // 내장 클래스 (DailyTime)
    //──────────────────────────────────────────────

    public static class DailyTime {

        @SerializedName("start")
        private String start;

        @SerializedName("end")
        private String end;

        public DailyTime() {}

        public DailyTime(String start, String end) {
            this.start = start;
            this.end = end;
        }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }
}
