// src/main/java/com/team103/controller/AttendanceCheckInController.java
package com.team103.controller;

import com.team103.dto.CheckInRequest;
import com.team103.dto.CheckInResponse;
import com.team103.model.Attendance;
import com.team103.model.Course;
import com.team103.model.Student;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;
import com.team103.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceCheckInController {

    private final CourseRepository courseRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final MongoTemplate mongoTemplate;

    @Value("${attendance.lateAfterMin:5}")
    private int lateAfterMin;

    @Value("${attendance.absentAfterMin:20}")
    private int absentAfterMin;

    public AttendanceCheckInController(
            CourseRepository courseRepository,
            AttendanceRepository attendanceRepository,
            StudentRepository studentRepository,
            MongoTemplate mongoTemplate
    ) {
        this.courseRepository = courseRepository;
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(@RequestBody CheckInRequest req) {

        if (req == null || req.getStudentId() == null) {
            return ResponseEntity.badRequest().body("studentId 필요");
        }

        String studentId = req.getStudentId();
        String classId = req.getClassId(); // ✅ classId가 없으면 입구 출석 처리
        Integer academyNumberFromReq = req.getAcademyNumber(); // ✅ 새로 추가된 학원번호

        ZoneId KST = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(KST);
        LocalDate today = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        // ✅ 학생 정보 조회
        Student stu = studentRepository.findByStudentId(studentId);
        if (stu == null) {
            return ResponseEntity.badRequest().body("학생을 찾을 수 없음");
        }

        // ✅ classId가 없으면 학원 입구 출석 처리
        if (classId == null || classId.isEmpty()) {
            String ymd = today.toString();

            // Attendance(입구 출석) 기록
            Query q = new Query(Criteria.where("Date").is(ymd).and("Type").is("entrance"));
            Update up = new Update()
                    .setOnInsert("Type", "entrance")
                    .setOnInsert("Date", ymd)
                    .set("updatedAt", new Date());

            mongoTemplate.upsert(q, up, "attendances");

            Update pushEntry = new Update().push("Attendance_List", new HashMap<String, Object>() {{
                put("Student_ID", studentId);
                put("Status", "입구 출석");
                put("CheckIn_Time", nowTime.toString());
                put("Source", "tablet");
            }});

            mongoTemplate.findAndModify(q, pushEntry,
                    FindAndModifyOptions.options().upsert(true).returnNew(true),
                    Attendance.class, "attendances");

            // ✅ waiting_room 컬렉션에도 추가
            Map<String, Object> waitingEntry = new HashMap<>();
            waitingEntry.put("studentId", stu.getStudentId());

            Integer academyNumber = (academyNumberFromReq != null)
                    ? academyNumberFromReq
                    : ((stu.getAcademyNumbers() != null && !stu.getAcademyNumbers().isEmpty())
                        ? stu.getAcademyNumbers().get(0)
                        : null);

            waitingEntry.put("academyNumber", academyNumber);
            waitingEntry.put("studentName", stu.getStudentName());
            waitingEntry.put("school", stu.getSchool());
            waitingEntry.put("grade", String.valueOf(stu.getGrade()));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String formattedTime = now.toLocalDateTime().format(formatter);
            waitingEntry.put("checkedInAt", formattedTime);
            waitingEntry.put("status", "LOBBY");

            mongoTemplate.insert(waitingEntry, "waiting_room");

            CheckInResponse res = new CheckInResponse();
            res.setStatus("입구 출석");
            res.setClassId(null);
            res.setDate(ymd);
            res.setSessionStart(null);
            res.setSessionEnd(null);

            return ResponseEntity.ok(res);
        }

        // ✅ 기존 QR 출석 로직
        int dow = now.getDayOfWeek().getValue(); // 1=월..7=일
        List<Course> allCourses = courseRepository.findAll();

        // ✅ 오늘 해당 학생이 수강 중인 강의 필터링
        List<Course> todayCourses = new ArrayList<>();
        for (Course c : allCourses) {
            if (c.getStudents().contains(studentId)
                    && c.getDaysOfWeek() != null     // ✅ 수정됨
                    && c.getDaysOfWeek().contains(dow)) { // ✅ 수정됨
                todayCourses.add(c);
            }
        }

        if (todayCourses.isEmpty()) {
            return ResponseEntity.status(409).body("오늘은 해당 학생의 수업이 없음");
        }

        // ✅ 현재 시간 기준 매칭되는 강의 찾기
        Course matched = null;
        DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

        for (Course c : todayCourses) {
            LocalTime S = LocalTime.parse(c.getStartTime(), HHMM);
            LocalTime lateUntil = S.plusMinutes(absentAfterMin);
            LocalTime openFrom = S.minusMinutes(5);

            if (!nowTime.isBefore(openFrom) && !nowTime.isAfter(lateUntil)) {
                matched = c;
                break;
            }
        }

        if (matched == null) {
            return ResponseEntity.status(409).body("현재 시간에 해당하는 수업이 없음");
        }

        classId = matched.getClassId();

        // ✅ 출석/지각 상태 결정
        LocalTime S = LocalTime.parse(matched.getStartTime(), HHMM);
        LocalTime presentUntil = S.plusMinutes(lateAfterMin);
        String status = !nowTime.isAfter(presentUntil) ? "출석" : "지각";

        String ymd = today.toString();

        Query q = new Query(Criteria.where("Class_ID").is(classId).and("Date").is(ymd));
        Update up = new Update()
                .setOnInsert("Session_Start", matched.getStartTime())
                .setOnInsert("Session_End", matched.getEndTime())
                .set("updatedAt", new Date());

        mongoTemplate.upsert(q, up, "attendances");

        // ✅ 기존 학생 업데이트
        Update setEntry = new Update()
                .set("Attendance_List.$[s].Status", status)
                .set("Attendance_List.$[s].CheckIn_Time", nowTime.toString())
                .filterArray(Criteria.where("s.Student_ID").is(studentId));

        FindAndModifyOptions opts = FindAndModifyOptions.options().upsert(true).returnNew(true);
        Attendance updated = mongoTemplate.findAndModify(q, setEntry, opts, Attendance.class, "attendances");

        // ✅ 학생 엔트리가 없으면 새로 추가
        if (!hasStudentEntry(updated, studentId)) {
            Update pushEntry = new Update().push("Attendance_List", new HashMap<String, Object>() {{
                put("Student_ID", studentId);
                put("Status", status);
                put("CheckIn_Time", nowTime.toString());
                put("Source", "app");
            }});
            mongoTemplate.findAndModify(q, pushEntry, opts, Attendance.class, "attendances");
        }

        CheckInResponse res = new CheckInResponse();
        res.setStatus(status);
        res.setClassId(classId);
        res.setDate(ymd);
        res.setSessionStart(matched.getStartTime());
        res.setSessionEnd(matched.getEndTime());

        return ResponseEntity.ok(res);
    }

    private boolean hasStudentEntry(Attendance att, String studentId) {
        if (att == null || att.getAttendanceList() == null) return false;
        for (Object e : att.getAttendanceList()) {
            if (e instanceof Map<?, ?> m) {
                if (studentId.equals(String.valueOf(m.get("Student_ID")))) return true;
            }
        }
        return false;
    }
}
