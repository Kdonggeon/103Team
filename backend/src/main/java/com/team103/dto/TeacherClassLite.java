package com.team103.dto;

/** 프론트 TeacherMainPanel에서 쓰는 경량 카드 (수동 Builder 버전) */
public class TeacherClassLite {
    private String classId;
    private String className;
    private Integer roomNumber;   // null 가능
    private String startTime;     // "HH:mm"
    private String endTime;       // "HH:mm"

    public TeacherClassLite() {}

    public TeacherClassLite(String classId, String className, Integer roomNumber, String startTime, String endTime) {
        this.classId = classId;
        this.className = className;
        this.roomNumber = roomNumber;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /* ===== Builder ===== */
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String classId;
        private String className;
        private Integer roomNumber;
        private String startTime;
        private String endTime;

        public Builder classId(String v) { this.classId = v; return this; }
        public Builder className(String v) { this.className = v; return this; }
        public Builder roomNumber(Integer v) { this.roomNumber = v; return this; }
        public Builder startTime(String v) { this.startTime = v; return this; }
        public Builder endTime(String v) { this.endTime = v; return this; }

        public TeacherClassLite build() {
            return new TeacherClassLite(classId, className, roomNumber, startTime, endTime);
        }
    }

    /* ===== Getters / Setters ===== */
    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Integer getRoomNumber() { return roomNumber; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
