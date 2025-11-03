package com.team103.controller;

import com.team103.dto.CreateClassRequest;
import com.team103.dto.ScheduleItem;
import com.team103.dto.StudentSearchResponse;
import com.team103.dto.UpdateClassRequest;
import com.team103.model.Course;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.CourseRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 교사용 반/학생/스케줄 관리 컨트롤러 (원장도 접근 가능)
 * 프론트의 경로 혼재를 위해 두 prefix를 동시에 지원:
 *   - /api/teachers/**
 *   - /api/manage/teachers/**
 */
@RestController
@RequestMapping({"/api/teachers", "/api/manage/teachers"})
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('TEACHER','DIRECTOR')")
public class TeacherClassManageController {

    private final CourseRepository courseRepo;
    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;
    private final MongoTemplate mongo;

    public TeacherClassManageController(CourseRepository courseRepo,
                                        StudentRepository studentRepo,
                                        TeacherRepository teacherRepo,
                                        MongoTemplate mongo) {
        this.courseRepo = courseRepo;
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
        this.mongo = mongo;
    }

    /* ===================== 공통 가드/유틸 ===================== */

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        String needed = "ROLE_" + role;
        return auth.getAuthorities().stream().anyMatch(a -> needed.equals(a.getAuthority()));
    }

    /** path/body의 teacherId가 본인(authentication.name)인지(또는 원장 권한) 검사 */
    private void guardTeacherId(String teacherId, Authentication auth) {
        String me = (auth != null) ? auth.getName() : null;
        boolean director = hasRole(auth, "DIRECTOR");
        if (me == null || (!director && !teacherId.equals(me))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "teacherId mismatch");
        }
    }

    /** 해당 수업의 소유 교사만 접근 허용(또는 원장) */
    private void guardCourseOwner(Course c, Authentication auth) {
        if (c == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "class not found");
        String me = (auth != null) ? auth.getName() : null;
        boolean director = hasRole(auth, "DIRECTOR");
        if (me == null || (!director && !c.getTeacherId().equals(me))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your class");
        }
    }

    /* ===================== 반 목록 (교사별) ===================== */

    @GetMapping("/{teacherId}/classes")
    public List<Course> listMyClasses(@PathVariable String teacherId, Authentication auth) {
        guardTeacherId(teacherId, auth);
        return courseRepo.findByTeacherId(teacherId);
    }

    /* ===================== 반 생성 ===================== */

    @PostMapping("/classes")
    public ResponseEntity<?> createClass(@RequestBody CreateClassRequest req, Authentication auth) {
        if (req.getTeacherId() == null || req.getClassName() == null || req.getAcademyNumber() == null) {
            return ResponseEntity.badRequest().body("teacherId, className, academyNumber 필요");
        }

        // 로그인 교사 본인만 생성 가능(또는 원장)
        guardTeacherId(req.getTeacherId(), auth);

        // 교사-학원 번호 권한 체크
        Teacher t = teacherRepo.findByTeacherId(req.getTeacherId());
        if (t == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("교사 없음");
        }
        List<Integer> allowed = (t.getAcademyNumbers() != null && !t.getAcademyNumbers().isEmpty())
                ? t.getAcademyNumbers()
                : Collections.emptyList();
        if (!allowed.contains(req.getAcademyNumber())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("학원번호 권한 없음");
        }

        // 저장
        Course c = new Course();
        c.setId(null);
        c.setClassId("class" + System.currentTimeMillis());
        c.setClassName(req.getClassName());
        c.setTeacherId(req.getTeacherId());
        c.setAcademyNumber(req.getAcademyNumber());
        c.setStudents(new ArrayList<>());

        // ✅ 강의실(여러개) 처리: roomNumbers 우선, 없으면 roomNumber
        if (req.getRoomNumbers() != null && !req.getRoomNumbers().isEmpty()) {
            c.setRoomNumbers(new ArrayList<>(req.getRoomNumbers()));
            c.setRoomNumber(req.getRoomNumbers().get(0)); // 호환 필드도 채움
        } else {
            c.setRoomNumber(req.getRoomNumber());
            if (req.getRoomNumber() != null) {
                c.setRoomNumbers(List.of(req.getRoomNumber()));
            }
        }

        // 시간표(있으면 저장)
        c.setStartTime(req.getStartTime());
        c.setEndTime(req.getEndTime());
        if (req.getDaysOfWeek() != null) c.setDaysOfWeek(new ArrayList<>(req.getDaysOfWeek()));
        c.setSchedule(req.getSchedule());

        return ResponseEntity.ok(courseRepo.save(c));
    }

    /* ===================== 반 상세 ===================== */

    @GetMapping("/classes/{classId}")
    public ResponseEntity<Course> getClassDetail(@PathVariable String classId, Authentication auth) {
        return courseRepo.findByClassId(classId)
                .map(c -> {
                    guardCourseOwner(c, auth);
                    return ResponseEntity.ok(c);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /* ===================== 반 수정 (이름/방/학원 + 시간표) ===================== */

    @PatchMapping("/classes/{classId}")
    public ResponseEntity<?> updateClass(@PathVariable String classId,
                                         @RequestBody UpdateClassRequest req,
                                         Authentication auth) {
        return courseRepo.findByClassId(classId).map(c -> {
            guardCourseOwner(c, auth);

            if (req.getClassName() != null) c.setClassName(req.getClassName());
            if (req.getAcademyNumber() != null) c.setAcademyNumber(req.getAcademyNumber());

            // ✅ 강의실(여러개) 패치
            if (req.getRoomNumbers() != null) {
                List<Integer> copy = (req.getRoomNumbers().isEmpty()) ? null : new ArrayList<>(req.getRoomNumbers());
                c.setRoomNumbers(copy);
                c.setRoomNumber((copy == null || copy.isEmpty()) ? null : copy.get(0)); // 호환 필드 동기화
            } else if (req.getRoomNumber() != null) {
                c.setRoomNumber(req.getRoomNumber());
                c.setRoomNumbers((req.getRoomNumber() == null) ? null : List.of(req.getRoomNumber()));
            }

            // 시간표 필드
            if (req.getStartTime() != null) c.setStartTime(req.getStartTime());
            if (req.getEndTime() != null) c.setEndTime(req.getEndTime());
            if (req.getDaysOfWeek() != null) c.setDaysOfWeek(new ArrayList<>(req.getDaysOfWeek()));
            if (req.getSchedule() != null) c.setSchedule(req.getSchedule());

            courseRepo.save(c);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    /* ===================== 반 삭제 ===================== */

    @DeleteMapping("/classes/{classId}")
    public ResponseEntity<?> deleteClass(@PathVariable String classId, Authentication auth) {
        return courseRepo.findByClassId(classId).map(c -> {
            guardCourseOwner(c, auth);
            courseRepo.delete(c);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    /* ===================== 반에 학생 추가/제거 ===================== */

    /** 쿼리파라미터 방식 (?studentId=) */
    @PostMapping("/classes/{classId}/students")
    public ResponseEntity<?> addStudent(@PathVariable String classId,
                                        @RequestParam String studentId,
                                        Authentication auth) {
        return addStudentInternal(classId, studentId, auth);
    }

    /** 경로변수 방식 (/students/{studentId}) */
    @PostMapping("/classes/{classId}/students/{studentId}")
    public ResponseEntity<?> addStudentPath(@PathVariable String classId,
                                            @PathVariable String studentId,
                                            Authentication auth) {
        return addStudentInternal(classId, studentId, auth);
    }

    private ResponseEntity<?> addStudentInternal(String classId, String studentId, Authentication auth) {
        return courseRepo.findByClassId(classId).map(c -> {
            guardCourseOwner(c, auth);
            List<String> st = (c.getStudents() == null) ? new ArrayList<>() : new ArrayList<>(c.getStudents());
            if (!st.contains(studentId)) st.add(studentId);
            c.setStudents(st);
            courseRepo.save(c);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/classes/{classId}/students/{studentId}")
    public ResponseEntity<?> removeStudent(@PathVariable String classId,
                                           @PathVariable String studentId,
                                           Authentication auth) {
        return courseRepo.findByClassId(classId).map(c -> {
            guardCourseOwner(c, auth);
            if (c.getStudents() != null) {
                c.setStudents(c.getStudents().stream()
                        .filter(id -> !studentId.equals(id))
                        .collect(Collectors.toList()));
                courseRepo.save(c);
            }
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    /* ===================== 학생 검색 ===================== */

    @GetMapping("/students/search")
    public List<StudentSearchResponse> searchStudents(@RequestParam Integer academyNumber,
                                                      @RequestParam(required = false, defaultValue = "") String q,
                                                      @RequestParam(required = false) Integer grade) {
        final String regex = (q == null || q.isBlank()) ? ".*" : q;
        final String academyNumberStr = String.valueOf(academyNumber);

        final List<Student> list = (grade == null)
                ? studentRepo.findByAcademyLooseAndNameLike(academyNumber, academyNumberStr, regex)
                : studentRepo.findByAcademyLooseAndGradeAndNameLike(academyNumber, academyNumberStr, grade, regex);

        return list.stream().map(s -> {
            StudentSearchResponse r = new StudentSearchResponse();
            r.setStudentId(s.getStudentId());
            r.setStudentName(s.getStudentName());
            r.setGrade(s.getGrade());
            Integer academy = (s.getAcademyNumbers() != null && !s.getAcademyNumbers().isEmpty())
                    ? s.getAcademyNumbers().get(0) : null;
            r.setAcademyNumber(academy);
            return r;
        }).collect(Collectors.toList());
    }

    /* ===================== 교사용: 학생 정보 수정(아이디 변경 포함) ===================== */

    @PatchMapping("/students/{studentId}")
    public ResponseEntity<?> updateStudentByTeacher(@PathVariable String studentId,
                                                    @RequestBody Map<String, Object> body,
                                                    Authentication auth) {
        // 인증 체크
        if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED");

        // 1) 학생 조회
        Student st = studentRepo.findByStudentId(studentId);
        if (st == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("student not found");

        // 2) 권한 체크: 교사 또는 원장 + 학원번호 교집합
        Teacher me = teacherRepo.findByTeacherId(auth.getName());
        boolean director = hasRole(auth, "DIRECTOR");
        if (!director) {
            if (me == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("NO_PERMISSION");
            List<Integer> mine = Optional.ofNullable(me.getAcademyNumbers()).orElse(List.of());
            List<Integer> students = Optional.ofNullable(st.getAcademyNumbers()).orElse(List.of());
            boolean ok = false;
            for (Integer n : students) if (mine.contains(n)) { ok = true; break; }
            if (!ok) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("NO_PERMISSION");
        }

        // 3) 기본 필드 적용
        if (body.containsKey("studentName")) st.setStudentName(Objects.toString(body.get("studentName"), st.getStudentName()));
        if (body.containsKey("Student_Name")) st.setStudentName(Objects.toString(body.get("Student_Name"), st.getStudentName()));
        if (body.containsKey("school")) st.setSchool(Objects.toString(body.get("school"), st.getSchool()));
        if (body.containsKey("School")) st.setSchool(Objects.toString(body.get("School"), st.getSchool()));
        if (body.containsKey("grade")) st.setGrade(body.get("grade") == null ? null : Integer.valueOf(body.get("grade").toString()));
        if (body.containsKey("Grade")) st.setGrade(body.get("Grade") == null ? null : Integer.valueOf(body.get("Grade").toString()));
        if (body.containsKey("gender")) st.setGender(Objects.toString(body.get("gender"), st.getGender()));
        if (body.containsKey("Gender")) st.setGender(Objects.toString(body.get("Gender"), st.getGender()));

        // 4) 아이디 변경 처리
        String newId = null;
        if (body.containsKey("studentId")) newId = Objects.toString(body.get("studentId"), null);
        if (body.containsKey("Student_ID")) newId = Objects.toString(body.get("Student_ID"), newId);

        if (newId != null && !newId.isBlank() && !newId.equals(studentId)) {
            if (studentRepo.existsByStudentId(newId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("DUPLICATE_STUDENT_ID");
            }

            // 4-1) 학생 문서 저장(아이디 변경)
            st.setStudentId(newId);
            studentRepo.save(st);

            // 4-2) 수업 students 배열 값 교체
            List<Course> courses = courseRepo.findByStudentsContaining(studentId);
            for (Course c : courses) {
                List<String> arr = new ArrayList<>(Optional.ofNullable(c.getStudents()).orElse(List.of()));
                for (int i = 0; i < arr.size(); i++) if (studentId.equals(arr.get(i))) arr.set(i, newId);
                c.setStudents(arr);
                courseRepo.save(c);
            }

            // 4-3) 부모 문서의 관련 필드 교체 — 위치 연산자($) 사용
            mongo.updateMulti(
                    new Query(Criteria.where("Student_ID_List").is(studentId)),
                    new Update().set("Student_ID_List.$", newId),
                    "parents"
            );
            mongo.updateMulti(
                    new Query(Criteria.where("studentIdList").is(studentId)),
                    new Update().set("studentIdList.$", newId),
                    "parents"
            );
            mongo.updateMulti(
                    new Query(Criteria.where("children.Student_ID").is(studentId)),
                    new Update().set("children.$.Student_ID", newId),
                    "parents"
            );
            mongo.updateMulti(
                    new Query(Criteria.where("children.studentId").is(studentId)),
                    new Update().set("children.$.studentId", newId),
                    "parents"
            );
            mongo.updateMulti(
                    new Query(Criteria.where("students.Student_ID").is(studentId)),
                    new Update().set("students.$.Student_ID", newId),
                    "parents"
            );
            mongo.updateMulti(
                    new Query(Criteria.where("students.studentId").is(studentId)),
                    new Update().set("students.$.studentId", newId),
                    "parents"
            );

            return ResponseEntity.ok(Map.of("studentId", newId));
        } else {
            // 일반 필드만 저장
            studentRepo.save(st);
            return ResponseEntity.ok().build();
        }
    }

    /* ===================== 교사용: 학부모 정보 수정 ===================== */

    @PatchMapping("/parents/{parentId}")
    public ResponseEntity<?> updateParentByTeacher(@PathVariable String parentId,
                                                   @RequestBody Map<String, Object> body,
                                                   Authentication auth) {
        if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED");

        // Parent 문서 조회(Parent_ID / parentsId / parentId 아무거나)
        Query findQ = new Query(new Criteria().orOperator(
                Criteria.where("Parent_ID").is(parentId),
                Criteria.where("parentsId").is(parentId),
                Criteria.where("parentId").is(parentId)
        ));
        @SuppressWarnings("rawtypes")
        Map parentDoc = mongo.findOne(findQ, Map.class, "parents");
        if (parentDoc == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("parent not found");

        // 권한 체크: 교사 소속 학원번호 ∩ 자녀들의 학원번호
        Teacher me = teacherRepo.findByTeacherId(auth.getName());
        boolean director = hasRole(auth, "DIRECTOR");
        if (!director) {
            if (me == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("NO_PERMISSION");
            List<Integer> mine = Optional.ofNullable(me.getAcademyNumbers()).orElse(List.of());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) (
                    parentDoc.getOrDefault("children",
                            parentDoc.getOrDefault("students", List.of()))
            );

            boolean ok = false;
            for (var ch : children) {
                Object raw = Optional.ofNullable(ch.get("Academy_Numbers"))
                        .orElse(Optional.ofNullable(ch.get("Academy_Number"))
                                .orElse(ch.get("academyNumbers")));
                List<Integer> nums = new ArrayList<>();
                if (raw instanceof List<?> l) {
                    for (Object v : l) try { nums.add(Integer.valueOf(v.toString())); } catch (Exception ignore) {}
                } else if (raw != null) {
                    try { nums.add(Integer.valueOf(raw.toString())); } catch (Exception ignore) {}
                }
                for (Integer n : nums) if (mine.contains(n)) { ok = true; break; }
                if (ok) break;
            }
            if (!ok) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("NO_PERMISSION");
        }

        // 업데이트 빌드
        Update up = new Update();
        if (body.containsKey("parentsName")) up.set("parentsName", body.get("parentsName"));
        if (body.containsKey("Parent_Name")) up.set("Parent_Name", body.get("Parent_Name"));
        if (body.containsKey("parentName"))  up.set("parentName",  body.get("parentName"));

        // 폰번호 다양한 키 대응 → 여러 필드 동시 세팅
        Object phone = null;
        for (String k : List.of("Parent_Phone_Number","Parents_Phone_Number","parentsPhoneNumber","ParentPhoneNumber","phone","mobile","phoneNumber")) {
            if (body.containsKey(k)) { phone = body.get(k); break; }
        }
        if (phone != null) {
            up.set("Parent_Phone_Number", phone);
            up.set("parentsPhoneNumber", phone);
            up.set("phone", phone);
            up.set("mobile", phone);
            up.set("phoneNumber", phone);
        }

        // 아이디 변경(선택)
        String newPid = null;
        for (String k : List.of("parentsId","Parent_ID","parentId")) {
            if (body.containsKey(k)) { newPid = Objects.toString(body.get(k), null); break; }
        }
        if (newPid != null && !newPid.isBlank() && !newPid.equals(parentId)) {
            up.set("Parent_ID", newPid);
            up.set("parentsId", newPid);
            up.set("parentId", newPid);
        }

        mongo.updateFirst(findQ, up, "parents");
        return ResponseEntity.ok(newPid != null ? Map.of("parentId", newPid) : Map.of("ok", true));
    }

    /* ======================================================================
     *                           스케줄 섹션
     *  schedules 컬렉션 없이 Course만으로 계산해서 내려줌 + 날짜(추가/취소) 토글
     * ====================================================================== */

    @GetMapping("/{teacherId}/schedules")
    public List<ScheduleItem> listSchedules(@PathVariable String teacherId,
                                            @RequestParam(required = false) String date, // YYYY-MM-DD
                                            @RequestParam(required = false) String from, // YYYY-MM-DD
                                            @RequestParam(required = false) String to,   // YYYY-MM-DD (exclusive)
                                            Authentication auth) {
        guardTeacherId(teacherId, auth);

        List<Course> classes = courseRepo.findByTeacherId(teacherId);
        if (classes == null || classes.isEmpty()) return List.of();

        DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate start, endEx;

        if (date != null && !date.isBlank()) {
            start = LocalDate.parse(date, F);
            endEx = start.plusDays(1);
        } else {
            if (from == null || to == null) {
                LocalDate today = LocalDate.now();
                start = today;
                endEx = today.plusDays(1);
            } else {
                start = LocalDate.parse(from, F);
                endEx = LocalDate.parse(to, F); // [from, to)
            }
        }

        List<ScheduleItem> out = new ArrayList<>();

        for (Course c : classes) {
            if (c.getStartTime() == null || c.getEndTime() == null) continue;

            List<Integer> dows = c.getDaysOfWeekInt(); // 1=월..7=일
            Set<String> cancelled = new HashSet<>(Optional.ofNullable(c.getCancelledDates()).orElse(List.of()));
            Set<String> extra = new HashSet<>(Optional.ofNullable(c.getExtraDates()).orElse(List.of()));

            for (LocalDate d = start; d.isBefore(endEx); d = d.plusDays(1)) {
                String ymd = d.format(F);
                int dow = d.getDayOfWeek().getValue(); // 1~7
                boolean regular = dows.contains(dow);

                if ((regular && !cancelled.contains(ymd)) || (!regular && extra.contains(ymd))) {
                    ScheduleItem si = new ScheduleItem();
                    si.setScheduleId(c.getClassId() + "@" + ymd);
                    si.setTeacherId(teacherId);
                    si.setDate(ymd);
                    si.setClassId(c.getClassId());
                    si.setTitle(c.getClassName());
                    si.setStartTime(c.getStartTime());
                    si.setEndTime(c.getEndTime());
                    // ✅ 여러 강의실 중 1순위로 표시
                    si.setRoomNumber(c.getPrimaryRoomNumber());
                    out.add(si);
                }
            }
        }

        out.sort(Comparator
                .comparing(ScheduleItem::getDate)
                .thenComparing(ScheduleItem::getStartTime)
                .thenComparing(ScheduleItem::getTitle));

        return out;
    }

    @PostMapping("/{teacherId}/classes/{classId}/schedule/toggle")
    public ResponseEntity<?> toggleDate(@PathVariable String teacherId,
                                        @PathVariable String classId,
                                        @RequestParam String date, // YYYY-MM-DD
                                        Authentication auth) {
        guardTeacherId(teacherId, auth);

        Course c = courseRepo.findByClassId(classId).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();

        guardCourseOwner(c, auth);

        int dow = LocalDate.parse(date).getDayOfWeek().getValue(); // 1~7
        boolean regular = c.getDaysOfWeekInt().contains(dow);

        if (regular) c.toggleCancelledDate(date);
        else c.toggleExtraDate(date);

        courseRepo.save(c);
        return ResponseEntity.ok().build();
    }
}
