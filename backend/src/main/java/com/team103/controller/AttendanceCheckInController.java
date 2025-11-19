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

    private final CourseRepository courseRepo;
    private final AttendanceRepository attRepo;
    private final StudentRepository studentRepo;
    private final MongoTemplate mongo;

    @Value("${attendance.lateAfterMin:15}")
    private int lateAfterMin;

    @Value("${attendance.absentAfterMin:20}")
    private int absentAfterMin;

    private static final String COLL_ATT = "attendances";
    private static final String COLL_WAIT = "waiting_room";

    public AttendanceCheckInController(
            CourseRepository courseRepo,
            AttendanceRepository attRepo,
            StudentRepository studentRepo,
            MongoTemplate mongo
    ) {
        this.courseRepo = courseRepo;
        this.attRepo = attRepo;
        this.studentRepo = studentRepo;
        this.mongo = mongo;
    }

    /** =========================================================
     *  🚪 1) Entrance 입구 출석 (classId 없음)
     * ========================================================= */
    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(@RequestBody CheckInRequest req) {

        if (req == null || req.getStudentId() == null)
            return ResponseEntity.badRequest().body("studentId 필요");

        String studentId = req.getStudentId();
        String classId = req.getClassId();
        Integer academyReq = req.getAcademyNumber();

        ZoneId KST = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(KST);
        String ymd = now.toLocalDate().toString();
        String hm = now.toLocalTime().toString();

        // 학생 조회
        Student stu = studentRepo.findByStudentId(studentId);
        if (stu == null) return ResponseEntity.badRequest().body("학생 없음");

        /* =========================================================
         *  🎫 classId 없음 = Entrance 입구 출석
         * ========================================================= */
        if (classId == null || classId.isBlank()) {

            // entrance 문서 upsert
            Query q = new Query(
                    Criteria.where("Date").is(ymd)
                            .and("Type").is("entrance")
            );
            Update up = new Update()
                    .setOnInsert("Type", "entrance")
                    .setOnInsert("Date", ymd)
                    .set("updatedAt", new Date());

            mongo.upsert(q, up, COLL_ATT);

            // 리스트에 추가 (중복 push 방지: 그냥 push 허용 → seatBoard에서 처리)
            Update push = new Update().push("Attendance_List", Map.of(
                    "Student_ID", studentId,
                    "Status", "입구 출석",
                    "CheckIn_Time", hm,
                    "Source", "tablet"
            ));
            mongo.updateFirst(q, push, COLL_ATT);

            // waiting_room 업데이트
            Integer academyNumber = academyReq != null
                    ? academyReq
                    : stu.getAcademyNumbers() != null && !stu.getAcademyNumbers().isEmpty()
                        ? stu.getAcademyNumbers().get(0)
                        : null;

            if (academyNumber != null) {
                Query wq = new Query(
                        Criteria.where("Academy_Number").is(academyNumber)
                                .and("Student_ID").is(studentId)
                );
                Update wup = new Update()
                        .set("Student_ID", studentId)
                        .set("Academy_Number", academyNumber)
                        .set("Checked_In_At", now.toLocalDateTime().toString())
                        .set("Status", "LOBBY")
                        .set("Student_Name", stu.getStudentName())
                        .set("School", stu.getSchool())
                        .set("Grade", stu.getGrade());

                mongo.upsert(wq, wup, COLL_WAIT);
            }

            CheckInResponse r = new CheckInResponse();
            r.setStatus("입구 출석");
            r.setDate(ymd);
            return ResponseEntity.ok(r);
        }

        /* =========================================================
         *  📌 2) QR 수업 출석
         * ========================================================= */
        Course course = courseRepo.findByClassId(classId).orElse(null);
        if (course == null) return ResponseEntity.badRequest().body("수업 없음");

        // 오늘 수업 시간 결정
        DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

        Course.DailyTime dt = course.getTimeFor(ymd);
        String sStr = dt != null && dt.getStart() != null ? dt.getStart() : course.getStartTime();
        String eStr = dt != null && dt.getEnd() != null ? dt.getEnd() : course.getEndTime();

        if (sStr == null) return ResponseEntity.status(409).body("수업 시작 시간 없음");

        LocalTime start = LocalTime.parse(sStr, HHMM);
        LocalTime presentUntil = start.plusMinutes(lateAfterMin);  // 출석
        LocalTime lateUntil = start.plusMinutes(absentAfterMin);   // 지각 → 이후는 결석

        LocalDateTime sDateTime = LocalDateTime.of(now.toLocalDate(), start);
        LocalDateTime nowLt = now.toLocalDateTime();

        // 오픈 시간 제한
        if (nowLt.isBefore(sDateTime.minusMinutes(5)))
            return ResponseEntity.status(409).body("아직 출석 오픈 전");

        if (nowLt.isAfter(LocalDateTime.of(now.toLocalDate(), lateUntil)))
            return ResponseEntity.status(409).body("결석 처리 시간 이후");

        String status = nowLt.isAfter(LocalDateTime.of(now.toLocalDate(), presentUntil))
                ? "지각"
                : "출석";

        /* =========================================================
         *  Attendance 문서 upsert
         * ========================================================= */
        Attendance att = attRepo.findFirstByClassIdAndDate(classId, ymd);
        if (att == null) {
            att = new Attendance();
            att.setClassId(classId);
            att.setDate(ymd);
            att.setAttendanceList(new ArrayList<>());
            att.setSeatAssignments(new ArrayList<>());
        }

        // 학생 상태 업데이트 (+ 중복 방지)
        boolean found = false;
        for (Attendance.Item it : att.getAttendanceList()) {
            if (it.getStudentId().equals(studentId)) {
                it.setStatus(status);
                it.setCheckInTime(hm);
                found = true;
                break;
            }
        }

        if (!found) {
            Attendance.Item it = new Attendance.Item();
            it.setStudentId(studentId);
            it.setStatus(status);
            it.setCheckInTime(hm);
            att.getAttendanceList().add(it);
        }

        attRepo.save(att);

        // Entrance waiting_room 제거 (해당 수업에만)
        Integer academyNumber = course.getAcademyNumber();
        if (academyNumber != null) {
            Query del = new Query(
                    Criteria.where("Academy_Number").is(academyNumber)
                            .and("Student_ID").is(studentId)
            );
            mongo.remove(del, COLL_WAIT);
        }

        // 응답 구성
        CheckInResponse r = new CheckInResponse();
        r.setStatus(status);
        r.setClassId(classId);
        r.setDate(ymd);
        r.setSessionStart(sStr);
        r.setSessionEnd(eStr);

        return ResponseEntity.ok(r);
    }
}
