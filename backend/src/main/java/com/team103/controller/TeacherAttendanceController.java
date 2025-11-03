// src/main/java/com/team103/controller/TeacherAttendanceController.java
package com.team103.controller;

import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/teachers/classes")
@CrossOrigin(origins = "*")
public class TeacherAttendanceController {

    private final MongoTemplate mongoTemplate;

    public TeacherAttendanceController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // 프런트가 기대하는 최소 응답: studentId, status 만
    public static record AttendanceRow(String studentId, String status) {}

    /**
     * 수업별 출석 조회
     * - 컬렉션: attendances
     * - 문서 구조 예: { Class_ID, Date, Attendance_List: [{ Student_ID, Status }, ...] }
     * - date 없으면 오늘(yyyy-MM-dd)
     */
    @GetMapping("/{classId}/attendance")
    public ResponseEntity<List<AttendanceRow>> getAttendanceByClass(
            @PathVariable String classId,
            @RequestParam(required = false) String date
    ) {
        final String ymd = (date == null || date.isBlank())
                ? java.time.LocalDate.now().toString()
                : date;

        // Class_ID + Date 로 1건 조회
        Query q = new Query(Criteria.where("Class_ID").is(classId).and("Date").is(ymd));
        Document doc = mongoTemplate.findOne(q, Document.class, "attendances");

        List<AttendanceRow> rows = new ArrayList<>();
        if (doc != null) {
            // Attendance_List 꺼냄(없으면 빈 리스트)
            List<?> list = doc.getList("Attendance_List", Object.class);
            if (list != null) {
                for (Object o : list) {
                    if (o instanceof Document d) {
                        String sid = firstNonNullString(d, "Student_ID", "studentId", "StudentId", "student_id");
                        String status = firstNonNullString(d, "Status", "status");
                        if (sid != null || status != null) {
                            rows.add(new AttendanceRow(sid, status));
                        }
                    }
                }
            }
        }

        return ResponseEntity.ok(rows);
    }

    /** 여러 키 후보 중 처음 나오는 문자열 값을 반환 */
    private static String firstNonNullString(Document d, String... keys) {
        for (String k : keys) {
            Object v = d.get(k);
            if (v == null) continue;
            if (v instanceof String s) return s;
            return String.valueOf(v);
        }
        return null;
    }
}
