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

@RestController
@RequestMapping("/api/teachers") // ★★ 기존 /api/manage/teachers → /api/teachers 로 변경
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('TEACHER','DIRECTOR')") // 클래스 전체 권한
public class TeacherClassManageController {

    private final CourseRepository courseRepo;
    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;

    public TeacherClassManageController(CourseRepository courseRepo,
                                        StudentRepository studentRepo,
                                        TeacherRepository teacherRepo) {
        this.courseRepo = courseRepo;
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
    }

    /* ===================== 공통 가드 ===================== */

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

        // 교사-학원 번호 권한 체크 (단일/복수 겸용)
        Teacher t = teacherRepo.findByTeacherId(req.getTeacherId());
        if (t == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("교사 없음");
        }
        List<Integer> allowed = (t.getAcademyNumbers() != null && !t.getAcademyNumbers().isEmpty())
                ? t.getAcademyNumbers()
                : Collections.emptyList();
        if (req.getAcademyNumber() == null || !allowed.contains(req.getAcademyNumber())) {
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

        // 기본 시간표 필드(있으면 저장)
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

            // 기본 정보
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

    /* ===================== 반에 학생 추가 ===================== */
    @PostMapping("/classes/{classId}/students")
    public ResponseEntity<?> addStudent(@PathVariable String classId,
                                        @RequestParam String studentId,
                                        Authentication auth) {
        return courseRepo.findByClassId(classId).map(c -> {
            guardCourseOwner(c, auth);
            List<String> st = (c.getStudents() == null) ? new ArrayList<>() : new ArrayList<>(c.getStudents());
            if (!st.contains(studentId)) st.add(studentId);
            c.setStudents(st);
            courseRepo.save(c);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    /* ===================== 반에서 학생 제거 ===================== */
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
                                                      @RequestParam String q,
                                                      @RequestParam(required = false) Integer grade) {
        String regex = (q == null || q.isBlank()) ? ".*" : q;
        List<Student> list = (grade == null)
                ? studentRepo.findByAcademyAndNameLike(academyNumber, regex)
                : studentRepo.findByAcademyAndGradeAndNameLike(academyNumber, grade, regex);

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

    /* ===================== 스케줄 계산 (Course 기반) ===================== */
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
