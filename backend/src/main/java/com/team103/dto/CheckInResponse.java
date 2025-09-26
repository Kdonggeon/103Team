package com.team103.dto;

public class CheckInResponse {
    private String status;       // "present" | "late"
    private String classId;
    private String date;         
    private String sessionStart; 
    private String sessionEnd;  

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getSessionStart() { return sessionStart; }
    public void setSessionStart(String sessionStart) { this.sessionStart = sessionStart; }
    public String getSessionEnd() { return sessionEnd; }
    public void setSessionEnd(String sessionEnd) { this.sessionEnd = sessionEnd; }
}
