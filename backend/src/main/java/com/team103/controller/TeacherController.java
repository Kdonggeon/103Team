// C:\project\103Team-sub\backend\src\main\java\com\team103\controller\TeacherController.java
package com.team103.controller;

import com.team103.dto.FindIdRequest;
import com.team103.model.Teacher;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;
import com.team103.repository.TeacherRepository;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherRepository teacherRepo;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    @Autowired
    private CourseRepository courseRepo;       // 기존 필드 유지
    @Autowired
    private AttendanceRepository attendanceRepo;

    @Autowired
    public TeacherController(TeacherRepository teacherRepo,
                             PasswordEncoder passwordEncoder,
                             MongoTemplate mongoTemplate) {
        this.teacherRepo = teacherRepo;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
    }

    /** 교사 전체 조회 */
    @GetMapping
    public List<Teacher> getAll() {
        return teacherRepo.findAll();
    }

    /** 교사 생성 (비밀번호 해시 후 저장) */
    @PostMapping
    public Teacher create(@RequestBody Teacher teacher) {
        String raw = teacher.getTeacherPw();
        if (raw != null && !raw.isBlank()) {
            teacher.setTeacherPw(passwordEncoder.encode(raw));
        }
        return teacherRepo.save(teacher);
    }

    /** 특정 교사 단건 조회 */
    @GetMapping("/{teacherId}")
    public ResponseEntity<Teacher> getOne(@PathVariable String teacherId) {
        Teacher t = teacherRepo.findByTeacherId(teacherId);
        return (t == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(t);
    }

    /** 🔹 교사 소속 학원 해제 (특정 학원번호 제거) */
    @PatchMapping("/{teacherId}/academies/detach")
    public ResponseEntity<Teacher> detachAcademy(@PathVariable String teacherId,
                                                 @RequestParam("academyNumber") Integer academyNumber) {
        if (academyNumber == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "academyNumber is required");
        }

        Teacher t = teacherRepo.findByTeacherId(teacherId);
        if (t == null) {
            return ResponseEntity.notFound().build();
        }

        List<Integer> current = t.getAcademyNumbers();
        if (current == null || current.isEmpty()) {
            // 이미 아무 소속도 없으면 그대로 반환
            return ResponseEntity.ok(t);
        }

        List<Integer> next = current.stream()
                .filter(n -> !Objects.equals(n, academyNumber))
                .distinct()
                .collect(Collectors.toList());

        t.setAcademyNumbers(next);
        Teacher saved = teacherRepo.save(t);
        return ResponseEntity.ok(saved);
    }

    /** FCM 토큰 업데이트 */
    @PutMapping("/{teacherId}/fcm-token")
    public ResponseEntity<Void> updateFcmToken(@PathVariable String teacherId,
                                               @RequestParam("token") String token) {
        Teacher t = teacherRepo.findByTeacherId(teacherId);
        if (t == null) return ResponseEntity.notFound().build();
        t.setFcmToken((token == null || token.isBlank()) ? null : token);
        teacherRepo.save(t);
        return ResponseEntity.ok().build();
    }

    /** 아이디 찾기 (이름 + 전화번호) */
    @PostMapping("/find_id")
    public ResponseEntity<Map<String, String>> findTeacherId(@RequestBody FindIdRequest req) {
        String phone = req.normalizedPhone();
        Teacher t = teacherRepo.findByTeacherNameAndTeacherPhoneNumber(req.getName(), phone);
        if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        return ResponseEntity.ok(Map.of("username", t.getTeacherId()));
    }

    /** ⬇ 교사 과목 조회 (classes 컬렉션 기반) */
    @GetMapping("/{teacherId}/subjects")
    public ResponseEntity<Map<String, Object>> getSubjectsOfTeacher(
            @PathVariable String teacherId,
            @RequestParam(value = "academyNumber", required = false) Integer academyNumber) {

        Query q = new Query();
        q.addCriteria(Criteria.where("Teacher_ID").is(teacherId));
        if (academyNumber != null) {
            q.addCriteria(Criteria.where("Academy_Number").is(academyNumber));
        }

        List<Document> docs = mongoTemplate.find(q, Document.class, "classes");
        Set<String> subs = new LinkedHashSet<>();
        for (Document d : docs) {
            Object cn = d.get("Class_Name");
            if (cn != null) subs.add(String.valueOf(cn));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("teacherId", teacherId);
        body.put("subjects", new ArrayList<>(subs));
        return ResponseEntity.ok(body);
    }

    /** ⬇ 복합 조건 검색 (id/name/subject/academyNumber) + subjects/academyNumbers 집계 포함 */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchTeachers(
            @RequestParam(value = "teacherId", required = false) String teacherIdQ,
            @RequestParam(value = "name", required = false) String nameQ,
            @RequestParam(value = "subject", required = false) String subjectQ,
            @RequestParam(value = "academyNumber", required = false) Integer academyNumber) {

        final String idq = (teacherIdQ == null) ? null : teacherIdQ.trim().toLowerCase();
        final String nameq = (nameQ == null) ? null : nameQ.trim().toLowerCase();
        final String subj = (subjectQ == null) ? null : subjectQ.trim();

        // 1) 1차: Teacher 컬렉션에서 id/name 필터
        List<Teacher> teachers = teacherRepo.findAll();
        if (idq != null && !idq.isBlank()) {
            teachers = teachers.stream()
                    .filter(t -> t.getTeacherId() != null && t.getTeacherId().toLowerCase().contains(idq))
                    .collect(Collectors.toList());
        }
        if (nameq != null && !nameq.isBlank()) {
            teachers = teachers.stream()
                    .filter(t -> t.getTeacherName() != null && t.getTeacherName().toLowerCase().contains(nameq))
                    .collect(Collectors.toList());
        }

        // 2) 2차: classes에서 과목/학원 기준으로 교사ID 집계
        Map<String, Set<String>> subjectsByTid = new HashMap<>();
        Map<String, Set<Integer>> academyByTid = new HashMap<>();

        if (!teachers.isEmpty()) {
            List<String> tids = teachers.stream()
                    .map(Teacher::getTeacherId)
                    .filter(Objects::nonNull)
                    .toList();

            Query q = new Query();
            q.addCriteria(Criteria.where("Teacher_ID").in(tids));
            if (academyNumber != null) {
                q.addCriteria(Criteria.where("Academy_Number").is(academyNumber));
            }
            if (subj != null && !subj.isBlank()) {
                q.addCriteria(Criteria.where("Class_Name")
                        .regex(Pattern.compile(Pattern.quote(subj), Pattern.CASE_INSENSITIVE)));
            }

            List<Document> cls = mongoTemplate.find(q, Document.class, "classes");
            for (Document d : cls) {
                String tid = Objects.toString(d.get("Teacher_ID"), null);
                if (tid == null) continue;

                Object cn = d.get("Class_Name");
                if (cn != null) {
                    subjectsByTid.computeIfAbsent(tid, k -> new LinkedHashSet<>()).add(String.valueOf(cn));
                }
                Object an = d.get("Academy_Number");
                if (an != null) {
                    try {
                        int v = (an instanceof Number) ? ((Number) an).intValue()
                                : Integer.parseInt(String.valueOf(an));
                        academyByTid.computeIfAbsent(tid, k -> new LinkedHashSet<>()).add(v);
                    } catch (Exception ignored) {
                    }
                }
            }

            // subject/academyNumber가 요청되었으면, classes에서 걸러진 tid만 남김
            if ((subj != null && !subj.isBlank()) || academyNumber != null) {
                Set<String> ok = new HashSet<>();
                ok.addAll(subjectsByTid.keySet());
                ok.addAll(academyByTid.keySet());
                teachers = teachers.stream()
                        .filter(t -> ok.contains(t.getTeacherId()))
                        .collect(Collectors.toList());
            }
        }

        // 3) 응답 조립 (subjects/academyNumbers 포함)
        List<Map<String, Object>> out = new ArrayList<>();
        for (Teacher t : teachers) {
            String tid = t.getTeacherId();
            List<String> subs = subjectsByTid.containsKey(tid)
                    ? new ArrayList<>(subjectsByTid.get(tid))
                    : Collections.emptyList();
            List<Integer> acas = academyByTid.containsKey(tid)
                    ? new ArrayList<>(academyByTid.get(tid))
                    : Collections.emptyList();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", tid);
            row.put("name", t.getTeacherName());
            row.put("phone", t.getTeacherPhoneNumber());
            row.put("academyNumbers", acas);
            row.put("subjects", subs);
            out.add(row);
        }

        return ResponseEntity.ok(out);
    }
    
    @GetMapping("/_debug/detach-test")
    public ResponseEntity<String> debugDetach() {
        return ResponseEntity.ok("TeacherController detach DEBUG OK");
    }

}
