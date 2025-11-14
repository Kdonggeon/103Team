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

    @Value("${attendance.lateAfterMin:15}")
    private int lateAfterMin;

    @Value("${attendance.absentAfterMin:20}")
    private int absentAfterMin;

    private static final String ATTENDANCE_COLL = "attendances";
    private static final String WAITING_ROOM_COLL = "waiting_room";

    public AttendanceCheckInController(CourseRepository courseRepository,
                                       AttendanceRepository attendanceRepository,
                                       StudentRepository studentRepository,
                                       MongoTemplate mongoTemplate) {
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
        String classId = req.getClassId();
        Integer academyNumberFromReq = req.getAcademyNumber();

        ZoneId KST = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(KST);
        LocalDate today = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();
        String ymd = today.toString();

        // 학생 정보 조회
        Student stu = studentRepository.findByStudentId(studentId);
        if (stu == null) {
            return ResponseEntity.badRequest().body("학생을 찾을 수 없음");
        }

        // ================================
        // 1️⃣ classId 없으면 입구 출석 처리
        // ================================
        if (classId == null || classId.isEmpty()) {

            Query q = new Query(Criteria.where("Date").is(ymd).and("Type").is("entrance"));
            Update up = new Update()
                    .setOnInsert("Type", "entrance")
                    .setOnInsert("Date", ymd)
                    .set("updatedAt", new Date());

            mongoTemplate.upsert(q, up, ATTENDANCE_COLL);

            Update pushEntry = new Update().push("Attendance_List", Map.of(
                    "Student_ID", studentId,
                    "Status", "입구 출석",
                    "CheckIn_Time", nowTime.toString(),
                    "Source", "tablet"
            ));
            mongoTemplate.findAndModify(q, pushEntry,
                    FindAndModifyOptions.options().upsert(true).returnNew(true),
                    Attendance.class, ATTENDANCE_COLL);

            // waiting_room 추가
            Integer academyNumber = academyNumberFromReq != null ? academyNumberFromReq :
                    (stu.getAcademyNumbers() != null && !stu.getAcademyNumbers().isEmpty() ? stu.getAcademyNumbers().get(0) : null);

            Map<String, Object> waitingEntry = new HashMap<>();
            waitingEntry.put("studentId", studentId);
            waitingEntry.put("academyNumber", academyNumber);
            waitingEntry.put("studentName", stu.getStudentName());
            waitingEntry.put("school", stu.getSchool());
            waitingEntry.put("grade", stu.getGrade());
            waitingEntry.put("checkedInAt", now.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            waitingEntry.put("status", "LOBBY");

            mongoTemplate.insert(waitingEntry, WAITING_ROOM_COLL);

            CheckInResponse res = new CheckInResponse();
            res.setStatus("입구 출석");
            res.setClassId(null);
            res.setDate(ymd);
            res.setSessionStart(null);
            res.setSessionEnd(null);

            return ResponseEntity.ok(res);
        }

        // ================================
        // 2️⃣ QR 출석 로직
        // ================================
        Course course = courseRepository.findByClassId(classId).orElse(null);
        if (course == null) return ResponseEntity.badRequest().body("수업을 찾을 수 없음");

        DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

        // 수업 시작/종료 시간 결정 (DailyTime 우선)
        Course.DailyTime dt = course.getTimeFor(ymd);
        String sStr = dt != null && dt.getStart() != null ? dt.getStart() : course.getStartTime();
        String eStr = dt != null && dt.getEnd() != null ? dt.getEnd() : course.getEndTime();
        if (sStr == null || sStr.isEmpty()) return ResponseEntity.status(409).body("수업 시작 시간이 설정되지 않음");

        int dow = now.getDayOfWeek().getValue();
        if (dt == null && (course.getDaysOfWeekInt() == null || !course.getDaysOfWeekInt().contains(dow))) {
            return ResponseEntity.status(409).body("오늘은 해당 수업이 없음");
        }

        LocalTime startTime = LocalTime.parse(sStr, HHMM);
        LocalTime presentUntil = startTime.plusMinutes(lateAfterMin);
        LocalTime lateUntil = startTime.plusMinutes(absentAfterMin);
        LocalDateTime sDateTime = LocalDateTime.of(today, startTime);
        LocalDateTime nowLt = now.toLocalDateTime();
        LocalDateTime openFrom = sDateTime.minusMinutes(5);

        if (nowLt.isBefore(openFrom)) return ResponseEntity.status(409).body("아직 출석 오픈 전");
        if (nowLt.isAfter(LocalDateTime.of(today, lateUntil))) return ResponseEntity.status(409).body("결석 시간: 출석 불가");

        String status = nowLt.isAfter(LocalDateTime.of(today, presentUntil)) ? "LATE" : "PRESENT";

        // MongoDB Attendance 업데이트
        Query q = new Query(Criteria.where("Class_ID").is(classId).and("Date").is(ymd));
        Update upsert = new Update()
                .setOnInsert("Class_ID", classId)
                .setOnInsert("Date", ymd)
                .setOnInsert("Session_Start", sStr)
                .setOnInsert("Session_End", eStr != null ? eStr : startTime.plusMinutes(50).format(HHMM))
                .set("updatedAt", new Date());
        mongoTemplate.upsert(q, upsert, ATTENDANCE_COLL);

        // 학생 상태 업데이트
        Update setEntry = new Update()
                .set("Attendance_List.$[s].Status", status)
                .set("Attendance_List.$[s].CheckIn_Time", nowTime.toString())
                .filterArray(Criteria.where("s.Student_ID").is(studentId));
        FindAndModifyOptions opts = FindAndModifyOptions.options().upsert(true).returnNew(true);
        Attendance updated = mongoTemplate.findAndModify(q, setEntry, opts, Attendance.class, ATTENDANCE_COLL);

        // 학생 엔트리가 없으면 push
        if (!hasStudentEntry(updated, studentId)) {
            Update pushEntry = new Update().push("Attendance_List", Map.of(
                    "Student_ID", studentId,
                    "Status", status,
                    "CheckIn_Time", nowTime.toString(),
                    "Source", "app"
            ));
            mongoTemplate.findAndModify(q, pushEntry, opts, Attendance.class, ATTENDANCE_COLL);
        }

        CheckInResponse res = new CheckInResponse();
        res.setStatus(status);
        res.setClassId(classId);
        res.setDate(ymd);
        res.setSessionStart(sStr);
        res.setSessionEnd(eStr);

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
