// src/main/java/com/team103/controller/AttendanceCheckInController.java
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
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceCheckInController {

    private final CourseRepository courseRepository;
    private final AttendanceRepository attendanceRepository;
    private final MongoTemplate mongoTemplate;

    /** ✅ 시작 후 몇 분까지 '출석(PRESENT)'로 인정할지 (기본 15분) */
    @Value("${attendance.lateAfterMin:15}")
    private int lateAfterMin;

    /** 시작 후 몇 분이 지나면 '지각'도 불가(결석 시간)로 볼지 (기본 20분) */
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

        // ✅ 컬렉션명은 매핑(@Document)에서 자동으로 가져오기
        final String COLL = mongoTemplate.getCollectionName(Attendance.class);

        ZoneId KST = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(KST);
        LocalDate today = now.toLocalDate();
        int dow = now.getDayOfWeek().getValue(); // 1=월..7=일

        Course course = courseRepository.findByClassId(req.getClassId()).orElse(null);
        if (course == null) {
            return ResponseEntity.badRequest().body("수업을 찾을 수 없음");
        }

        DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
        String ymd = today.toString();

        // ✅ 날짜 오버라이드 우선 적용 + null 보호
        Course.DailyTime dt = course.getTimeFor(ymd);
        String sStr = (dt != null && dt.getStart() != null) ? dt.getStart() : course.getStartTime();
        String eStr = (dt != null && dt.getEnd()   != null) ? dt.getEnd()   : course.getEndTime();
        if (sStr == null || sStr.isEmpty()) {
            return ResponseEntity.status(409).body("수업 시작 시간이 설정되지 않음");
        }

        // ✅ 요일 체크: DailyTime 오버라이드가 있으면 요일 미일치라도 허용
        List<Integer> daysOfWeek = course.getDaysOfWeekInt();
        if (dt == null && (daysOfWeek == null || !daysOfWeek.contains(dow))) {
            return ResponseEntity.status(409).body("오늘은 해당 수업이 없음");
        }

        LocalTime S = LocalTime.parse(sStr, HHMM);
        LocalTime E = (eStr != null && !eStr.isEmpty()) ? LocalTime.parse(eStr, HHMM) : S.plusMinutes(50);

        LocalDateTime sDateTime     = LocalDateTime.of(today, S);
        LocalDateTime nowLt         = now.toLocalDateTime();
        LocalDateTime openFrom      = sDateTime.minusMinutes(5);            // 수업 5분 전부터 오픈
        LocalDateTime presentUntil  = sDateTime.plusMinutes(lateAfterMin);  // ✅ 15분까지 PRESENT
        LocalDateTime lateUntil     = sDateTime.plusMinutes(absentAfterMin);// 20분 이후 결석 시간

        if (nowLt.isBefore(openFrom)) return ResponseEntity.status(409).body("아직 출석 오픈 전");
        if (nowLt.isAfter(lateUntil)) return ResponseEntity.status(409).body("결석 시간: 출석 불가");

        // ✅ 상태값은 프론트와 일치하도록 영문 상수 사용
        String status = (!nowLt.isAfter(presentUntil)) ? "PRESENT" : "LATE";

        String classId = req.getClassId();
        String date = ymd;

        // 필드명은 모델의 @Field 이름과 일치해야 함
        Query q = new Query(Criteria.where("Class_ID").is(classId).and("Date").is(date));

        // 문서 껍데기(upsert)
        Update up = new Update()
                .setOnInsert("Class_ID", classId)
                .setOnInsert("Date", date)
                .setOnInsert("Session_Start", sStr)
                .setOnInsert("Session_End", (eStr != null && !eStr.isEmpty()) ? eStr : E.format(HHMM))
                .set("updatedAt", java.util.Date.from(now.toInstant()));

        // ⬇ 컬렉션 이름을 하드코딩하지 말고 COLL 사용
        mongoTemplate.upsert(q, up, COLL);

        // 기존 학생 항목 상태/시간 갱신 (arrayFilters)
        Update setEntry = new Update()
                .set("Attendance_List.$[s].Status", status)
                .set("Attendance_List.$[s].CheckIn_Time", nowLt.format(DateTimeFormatter.ISO_LOCAL_TIME))
                .set("Attendance_List.$[s].Source", "app")
                .filterArray(Criteria.where("s.Student_ID").is(req.getStudentId()));

        FindAndModifyOptions opts = FindAndModifyOptions.options().upsert(true).returnNew(true);
        Attendance updated = mongoTemplate.findAndModify(q, setEntry, opts, Attendance.class, COLL);

        // 학생 항목이 없으면 push
        if (!hasStudentEntry(updated, req.getStudentId())) {
            Update pushEntry = new Update().push("Attendance_List", new HashMap<String, Object>() {{
                put("Student_ID", req.getStudentId());
                put("Status", status);
                put("CheckIn_Time", nowLt.format(DateTimeFormatter.ISO_LOCAL_TIME));
                put("Source", "app");
            }});
            mongoTemplate.findAndModify(q, pushEntry, opts, Attendance.class, COLL);
        }

        CheckInResponse res = new CheckInResponse();
        res.setStatus(status); // PRESENT / LATE
        res.setClassId(classId);
        res.setDate(date);
        res.setSessionStart(sStr);
        res.setSessionEnd((eStr != null && !eStr.isEmpty()) ? eStr : E.format(HHMM));
        return ResponseEntity.ok(res);
    }

    private boolean hasStudentEntry(Attendance att, String studentId) {
        if (att == null || att.getAttendanceList() == null) return false;
        for (Object e : att.getAttendanceList()) {
            if (e == null) continue;
            if (e instanceof Attendance.Item item) {
                if (studentId.equals(item.getStudentId())) return true;
                continue;
            }
            if (e instanceof java.util.Map<?, ?> m) {
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

    private boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }
}
