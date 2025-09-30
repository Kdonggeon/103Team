package com.team103.controller;

import com.team103.dto.FindIdRequest;
import com.team103.model.Attendance;
import com.team103.model.Course;            // Class → Course
import com.team103.model.Teacher;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository; // ClassRepository → CourseRepository
import com.team103.repository.TeacherRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherRepository teacherRepo;


    @Autowired
    private CourseRepository courseRepo;

    @Autowired
    private AttendanceRepository attendanceRepo;


    public TeacherController(TeacherRepository teacherRepo) {
        this.teacherRepo = teacherRepo;
    }

    /** 교사 전체 조회 */
    @GetMapping
    public List<Teacher> getAll() {
        return teacherRepo.findAll();
    }

    /** 교사 생성 */
    @PostMapping
    public Teacher create(@RequestBody Teacher teacher) {
        return teacherRepo.save(teacher);
    }

    /** ✅ 수업 생성 */
    @PostMapping("/classes")
    public ResponseEntity<?> createClass(@RequestBody Course newCourse) {
        Course saved = courseRepo.save(newCourse);
        return ResponseEntity.ok(saved);
    }

    /** ✅ 수업에 학생 추가 */
    @PostMapping("/classes/{classId}/add-student")
    public ResponseEntity<?> addStudentToClass(@PathVariable String classId,
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

    /** ✅ 출석 등록 */
    @PostMapping("/attendance")
    public ResponseEntity<?> registerAttendance(@RequestBody Attendance attendance) {
        Attendance saved = attendanceRepo.save(attendance);
        return ResponseEntity.ok(saved);
    }

    /** ✅ 수업별 출석 현황 조회 */
    @GetMapping("/classes/{classId}/attendance")
    public ResponseEntity<?> getAttendance(@PathVariable String classId,
                                           @RequestParam String date) {
        List<Attendance> attendances = attendanceRepo.findByClassIdAndDate(classId, date);
        return ResponseEntity.ok(attendances);
    }

    /** ✅ 교사 ID로 수업 목록 조회 */
    @GetMapping("/{teacherId}/classes")
    public ResponseEntity<?> getTeacherClasses(@PathVariable String teacherId) {
        List<Course> classes = courseRepo.findByTeacherId(teacherId);
        return ResponseEntity.ok(classes);
    }
    
    @PutMapping("/{id}/fcm-token")
    public ResponseEntity<Void> updateFcmToken(@PathVariable("id") String teacherId,
                                               @RequestParam("token") String token) {
        Teacher t = teacherRepo.findByTeacherId(teacherId); // ← 이름 맞춤
        if (t == null) return ResponseEntity.notFound().build();
        t.setFcmToken((token == null || token.isBlank()) ? null : token);
        teacherRepo.save(t); // ← 이름 맞춤
        return ResponseEntity.ok().build();
    }

    /** ✅ 아이디 찾기 (이름 + 전화번호) */
    @PostMapping("/find_id")
    public ResponseEntity<Map<String,String>> findTeacherId(@RequestBody FindIdRequest req) {
        String phone = req.normalizedPhone();
        var t = teacherRepo.findByTeacherNameAndTeacherPhoneNumber(req.getName(), phone);
        if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        return ResponseEntity.ok(Map.of("username", t.getTeacherId()));
    }
}
