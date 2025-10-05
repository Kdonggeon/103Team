package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "classes")
public class Course {

    @Id
    private String id;                     // ObjectId 문자열 (기존 int _id 문서도 Spring이 매핑 가능)
                                           // 필요하면 Integer로 바꿔도 되지만, 일단 String이 충돌이 적습니다.

    // ====== 기존 수업 기본 정보 ======
    @Field("Class_ID")
    private String classId;

    @Field("Class_Name")
    private String className;

    @Field("Teacher_ID")
    private String teacherId;

    @Field("Students")
    private List<String> students;

    @Field("Schedule")
    private String schedule;

    @Field("Days_Of_Week")                 // 1=월 … 7=일
    private List<Integer> daysOfWeek;

    @Field("Start_Time")                   // "HH:mm"
    private String startTime;

    @Field("End_Time")                     // "HH:mm"
    private String endTime;

    // ====== 좌석/강의실 연동을 위한 추가 필드 ======
    @Field("Room_Number")
    private Integer roomNumber;            // 방 번호 (좌석 보드 매칭)

    @Field("Academy_Number")
    private Integer academyNumber;         // 학원 번호 (방 조회용)

    // 대시보드용 현재 수업 정보(네가 쓰던 구조 유지)
    @Field("Current_Class")
    private Room.CurrentClass current;

    // 수업-좌석 배정(좌석 번호 ↔ 학생 ID)
    @Field("Seats")
    private List<Room.Seat> seats;

    // --- getters/setters ---
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

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    public List<Integer> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<Integer> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Integer getRoomNumber() { return roomNumber; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }

    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }

    public Room.CurrentClass getCurrent() { return current; }
    public void setCurrent(Room.CurrentClass current) { this.current = current; }

    public List<Room.Seat> getSeats() { return seats; }
    public void setSeats(List<Room.Seat> seats) { this.seats = seats; }
}
