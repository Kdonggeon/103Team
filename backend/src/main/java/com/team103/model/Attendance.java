package com.team103.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "attendance")
public class Attendance {

  @Id
    private String id;

    @Field("Class_ID")
    private String classId;

    @Field("Date")
    private String date;

    @Field("Attendance_List")
    private List<AttendanceEntry> attendanceList;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public List<AttendanceEntry> getAttendanceList() { return attendanceList; }
    public void setAttendanceList(List<AttendanceEntry> attendanceList) { this.attendanceList = attendanceList; }
}


