// src/main/java/com/team103/controller/TeacherAttendanceController.java
package com.team103.controller;

import com.team103.model.Attendance;               // ✅ 추가
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
     * - 기본 컬렉션: attendance (@Document 매핑)
     * - 안전장치: 예전에 잘못 쓴 'attendances'에도 문서가 있으면 폴백 조회
     */
    @GetMapping("/{classId}/attendance")
    public ResponseEntity<List<AttendanceRow>> getAttendanceByClass(
            @PathVariable String classId,
            @RequestParam(required = false) String date
    ) {
        final String ymd = (date == null || date.isBlank())
                ? java.time.LocalDate.now().toString()
                : date;

        // ✅ 모델 매핑에서 컬렉션명 자동 획득
        final String COLL = mongoTemplate.getCollectionName(Attendance.class);
        final String LEGACY = "attendances"; // 폴백용(과거 오타)

        // Class_ID + Date 로 1건 조회
        Query q = new Query(Criteria.where("Class_ID").is(classId).and("Date").is(ymd));

        // 1) 정상 컬렉션 먼저
        Document doc = mongoTemplate.findOne(q, Document.class, COLL);
        // 2) 없으면 레거시 컬렉션에서도 한 번 더
        if (doc == null) {
            doc = mongoTemplate.findOne(q, Document.class, LEGACY);
        }

        List<AttendanceRow> rows = new ArrayList<>();
        if (doc != null) {
            List<?> list = doc.getList("Attendance_List", Object.class);
            if (list != null) {
                for (Object o : list) {
                    if (o instanceof Document d) {
                        String sid = firstNonNullString(d,
                                "Student_ID", "studentId", "StudentId", "student_id");
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
