package com.team103.controller;

import com.team103.dto.CheckInRequest;
import com.team103.dto.CheckInResponse;
import com.team103.model.Attendance;
import com.team103.model.Course;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceCheckInController {

    private final CourseRepository courseRepository;
    private final AttendanceRepository attendanceRepository;
    private final MongoTemplate mongoTemplate;

    @Value("${attendance.lateAfterMin:5}")
    private int lateAfterMin;

    @Value("${attendance.absentAfterMin:20}")
    private int absentAfterMin;

    public AttendanceCheckInController(CourseRepository courseRepository,
                                       AttendanceRepository attendanceRepository,
                                       MongoTemplate mongoTemplate) {
        this.courseRepository = courseRepository;
        this.attendanceRepository = attendanceRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(@RequestBody CheckInRequest req) {
        if (req == null || isEmpty(req.getClassId()) || isEmpty(req.getStudentId())) {
            return ResponseEntity.badRequest().body("classId / studentId 필요");
        }

        // (1) 오늘 KST
        ZoneId KST = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(KST);
        LocalDate today = now.toLocalDate();
        int dow = now.getDayOfWeek().getValue(); // 1=월..7=일

        // (2) 수업 조회
        Course course = courseRepository.findByClassId(req.getClassId());
        if (course == null) {
            return ResponseEntity.badRequest().body("수업을 찾을 수 없음");
        }
        List<Integer> daysOfWeek = course.getDaysOfWeek();
        if (daysOfWeek == null || !daysOfWeek.contains(dow)) {
            return ResponseEntity.status(409).body("오늘은 해당 수업이 없음");
        }

        // (3) 시작/끝 시각 계산
        DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
        String sStr = course.getStartTime(); // "HH:mm"
        String eStr = course.getEndTime();   // "HH:mm" or null
        LocalTime S = LocalTime.parse(sStr, HHMM);
        LocalTime E = (eStr != null && !eStr.isEmpty()) ? LocalTime.parse(eStr, HHMM) : S.plusMinutes(50);

        LocalDateTime sDateTime = LocalDateTime.of(today, S);
        LocalDateTime nowLt = now.toLocalDateTime();

        LocalDateTime openFrom     = sDateTime.minusMinutes(5);
        LocalDateTime presentUntil = sDateTime.plusMinutes(lateAfterMin);   // S+5
        LocalDateTime lateUntil    = sDateTime.plusMinutes(absentAfterMin); // S+20

        if (nowLt.isBefore(openFrom)) return ResponseEntity.status(409).body("아직 출석 오픈 전");
        if (nowLt.isAfter(lateUntil)) return ResponseEntity.status(409).body("결석 시간: 출석 불가");

        // 상태 결정
        String status = (!nowLt.isAfter(presentUntil)) ? "출석" : "지각"; // 표준 한글로

        // (4) attendance upsert (컬렉션명 통일: "attendances")
        String classId = req.getClassId();
        String date = today.format(DateTimeFormatter.ISO_DATE);

        Query q = new Query(Criteria.where("Class_ID").is(classId).and("Date").is(date));

        Update up = new Update()
                .setOnInsert("Session_Start", sStr)
                .setOnInsert("Session_End", (eStr != null && !eStr.isEmpty()) ? eStr : E.format(HHMM))
                .set("updatedAt", java.util.Date.from(now.toInstant()));

        mongoTemplate.upsert(q, up, "attendances"); // ✅ 컬렉션명 통일

        // arrayFilters로 엔트리 업데이트 시도
        Update setEntry = new Update()
                .set("Attendance_List.$[s].Status", status)
                .set("Attendance_List.$[s].CheckIn_Time", nowLt.format(DateTimeFormatter.ISO_LOCAL_TIME))
                .set("Attendance_List.$[s].Source", "app")
                .filterArray(Criteria.where("s.Student_ID").is(req.getStudentId()));

        FindAndModifyOptions opts = FindAndModifyOptions.options().upsert(true).returnNew(true);
        Attendance updated = mongoTemplate.findAndModify(q, setEntry, opts, Attendance.class, "attendances");

        // 엔트리가 없으면 push
        if (!hasStudentEntry(updated, req.getStudentId())) {
            Update pushEntry = new Update().push("Attendance_List", new HashMap<String, Object>() {{
                put("Student_ID", req.getStudentId());
                put("Status", status);
                put("CheckIn_Time", nowLt.format(DateTimeFormatter.ISO_LOCAL_TIME));
                put("Source", "app");
            }});
            mongoTemplate.findAndModify(q, pushEntry, opts, Attendance.class, "attendances");
        }

        // (5) 응답
        CheckInResponse res = new CheckInResponse();
        res.setStatus(status);
        res.setClassId(classId);
        res.setDate(date);
        res.setSessionStart(sStr);
        res.setSessionEnd((eStr != null && !eStr.isEmpty()) ? eStr : E.format(HHMM));
        return ResponseEntity.ok(res);
    }

    // ---- 유틸 (Map/Item 혼재 안전 처리) ----
    private boolean hasStudentEntry(Attendance att, String studentId) {
        if (att == null || att.getAttendanceList() == null) return false;
        for (Object e : att.getAttendanceList()) {
            if (e == null) continue;

            if (e instanceof Attendance.Item item) {
                if (studentId.equals(item.getStudentId())) return true;
                continue;
            }
            if (e instanceof Map<?, ?> m) {
                Object sid = m.get("Student_ID");
                if (sid != null && studentId.equals(String.valueOf(sid))) return true;
                continue;
            }
            try {
                Object sid = e.getClass().getMethod("getStudentId").invoke(e);
                if (sid != null && studentId.equals(String.valueOf(sid))) return true;
            } catch (Exception ignore) {}
        }
        return false;
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
