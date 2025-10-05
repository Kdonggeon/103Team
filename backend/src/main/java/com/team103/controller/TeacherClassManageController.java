package com.team103.controller;

import com.team103.dto.CreateClassRequest;
import com.team103.dto.StudentSearchResponse;
import com.team103.dto.UpdateClassRequest;
import com.team103.model.Course;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.CourseRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 교사 탭에서: 반 생성/목록/상세/수정, 학생 검색/추가/삭제
 */
@RestController
@RequestMapping("/api/manage/teachers")
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

    // ---- 반 목록 (교사별) ----
    @GetMapping("/{teacherId}/classes")
    public List<Course> listMyClasses(@PathVariable String teacherId) {
        return courseRepo.findByTeacherId(teacherId);
    }

    // ---- 반 생성 ----
    @PostMapping("/classes")
    public ResponseEntity<?> createClass(@RequestBody CreateClassRequest req) {
        if (req.getTeacherId() == null || req.getClassName() == null || req.getAcademyNumber() == null) {
            return ResponseEntity.badRequest().body("teacherId, className, academyNumber 필요");
        }
        // 교사-학원 번호 권한 체크(간단)
        Teacher t = teacherRepo.findByTeacherId(req.getTeacherId());
        if (t == null || t.getAcademyNumbers() == null || !t.getAcademyNumbers().contains(req.getAcademyNumber())) {
            return ResponseEntity.status(403).body("학원번호 권한 없음");
        }

        Course c = new Course();
        c.setId(null);
        c.setClassId(newClassId());
        c.setClassName(req.getClassName());
        c.setTeacherId(req.getTeacherId());
        c.setAcademyNumber(req.getAcademyNumber());
        c.setRoomNumber(req.getRoomNumber());
        c.setStudents(new ArrayList<>()); // 빈 수강생
        // daysOfWeek/startTime/endTime은 나중에 시간표에서 셋업
        return ResponseEntity.ok(courseRepo.save(c));
    }

    // ---- 반 상세 ----
    @GetMapping("/classes/{classId}")
    public ResponseEntity<Course> getClassDetail(@PathVariable String classId) {
        Course c = courseRepo.findByClassId(classId);
        return (c == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(c);
    }

    // ---- 반 수정(이름/방/학원) ----
    @PatchMapping("/classes/{classId}")
    public ResponseEntity<?> updateClass(@PathVariable String classId,
                                         @RequestBody UpdateClassRequest req) {
        Course c = courseRepo.findByClassId(classId);
        if (c == null) return ResponseEntity.notFound().build();
        if (req.getClassName() != null) c.setClassName(req.getClassName());
        if (req.getRoomNumber() != null) c.setRoomNumber(req.getRoomNumber());
        if (req.getAcademyNumber() != null) c.setAcademyNumber(req.getAcademyNumber());
        courseRepo.save(c);
        return ResponseEntity.ok().build();
    }

    // ---- 반 삭제(선택) ----
    @DeleteMapping("/classes/{classId}")
    public ResponseEntity<?> deleteClass(@PathVariable String classId) {
        Course c = courseRepo.findByClassId(classId);
        if (c == null) return ResponseEntity.notFound().build();
        courseRepo.delete(c);
        return ResponseEntity.ok().build();
    }

    // ---- 학생 검색(학원번호 + 이름 부분일치 + (선택)학년) ----
    @GetMapping("/students/search")
    public List<StudentSearchResponse> searchStudents(@RequestParam Integer academyNumber,
                                                      @RequestParam String q,
                                                      @RequestParam(required = false) Integer grade) {
        String regex = q == null || q.isBlank() ? ".*" : q;
        List<Student> list = (grade == null)
                ? studentRepo.findByAcademyAndNameLike(academyNumber, regex)
                : studentRepo.findByAcademyAndGradeAndNameLike(academyNumber, grade, regex);

        return list.stream().map(s -> {
            StudentSearchResponse r = new StudentSearchResponse();
            r.setStudentId(s.getStudentId());
            r.setStudentName(s.getStudentName()); // Student 엔티티의 getter에 맞춰주세요
            // s.getGrade(), s.getAcademyNumbers() 첫 값 등은 엔티티에 맞게 조정
            r.setGrade(s.getGrade());
            Integer academy = (s.getAcademyNumbers() != null && !s.getAcademyNumbers().isEmpty())
                    ? s.getAcademyNumbers().get(0) : null;
            r.setAcademyNumber(academy);
            return r;
        }).collect(Collectors.toList());
    }

    // ---- 반에 학생 추가 ----
    @PostMapping("/classes/{classId}/students")
    public ResponseEntity<?> addStudent(@PathVariable String classId,
                                        @RequestParam String studentId) {
        Course c = courseRepo.findByClassId(classId);
        if (c == null) return ResponseEntity.notFound().build();

        List<String> st = (c.getStudents() == null) ? new ArrayList<>() : new ArrayList<>(c.getStudents());
        if (!st.contains(studentId)) st.add(studentId);
        c.setStudents(st);
        courseRepo.save(c);
        return ResponseEntity.ok().build();
    }

    // ---- 반에서 학생 제거 ----
    @DeleteMapping("/classes/{classId}/students/{studentId}")
    public ResponseEntity<?> removeStudent(@PathVariable String classId,
                                           @PathVariable String studentId) {
        Course c = courseRepo.findByClassId(classId);
        if (c == null) return ResponseEntity.notFound().build();

        if (c.getStudents() != null) {
            c.setStudents(c.getStudents().stream()
                    .filter(id -> !studentId.equals(id))
                    .collect(Collectors.toList()));
            courseRepo.save(c);
        }
        return ResponseEntity.ok().build();
    }

    // ---- 유틸: classId 생성 ----
    private String newClassId() {
        return "class" + System.currentTimeMillis();
    }
}
