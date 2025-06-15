package com.team103.controller;

import com.team103.model.Attendance;
import com.team103.model.Teacher;
import com.team103.model.Course; // Class → Course 로 변경
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository; // ClassRepository → CourseRepository
import com.team103.repository.TeacherRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherRepository teacherRepo;

    @Autowired private CourseRepository courseRepo;
    @Autowired private AttendanceRepository attendanceRepo;

    public TeacherController(TeacherRepository teacherRepo) {
        this.teacherRepo = teacherRepo;
    }

    @GetMapping
    public List<Teacher> getAll() {
        return teacherRepo.findAll();
    }

    @PostMapping
    public Teacher create(@RequestBody Teacher teacher) {
        return teacherRepo.save(teacher);
    }

    // ✅ 1. 수업 생성
    @PostMapping("/classes")
    public ResponseEntity<?> createClass(@RequestBody Course newCourse) {
        Course saved = courseRepo.save(newCourse);
        return ResponseEntity.ok(saved);
    }

    // ✅ 2. 수업에 학생 추가
    @PostMapping("/classes/{classId}/add-student")
    public ResponseEntity<?> addStudentToClass(
            @PathVariable String classId,
            @RequestParam String studentId) {

        Course course = courseRepo.findByClassId(classId);
        if (course == null) return ResponseEntity.notFound().build();

        List<String> students = course.getStudents();

        if (!students.contains(studentId)) {
            students.add(studentId);
            course.setStudents(students);
            courseRepo.save(course);
        }

        return ResponseEntity.ok("학생 추가 완료");
    }

    // ✅ 3. 출석 등록
    @PostMapping("/attendance")
    public ResponseEntity<?> registerAttendance(@RequestBody Attendance attendance) {
        Attendance saved = attendanceRepo.save(attendance);
        return ResponseEntity.ok(saved);
    }

    // ✅ 4. 수업별 출석 현황 조회
    @GetMapping("/classes/{classId}/attendance")
    public ResponseEntity<?> getAttendance(
            @PathVariable String classId,
            @RequestParam String date) {

        List<Attendance> attendances = attendanceRepo.findByClassIdAndDate(classId, date);
        return ResponseEntity.ok(attendances);
    }
}
