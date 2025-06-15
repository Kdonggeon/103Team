package com.team103.controller;

import com.team103.dto.ParentSignupRequest;
import com.team103.model.Attendance;
import com.team103.model.Course;
import com.team103.model.Parent;
import com.team103.model.Student;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;
import com.team103.repository.ParentRepository;
import com.team103.repository.StudentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/parents")
@CrossOrigin(origins = "*")
public class ParentController {

    private final ParentRepository parentRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StudentRepository studentRepo;
    
    @Autowired
    private CourseRepository classRepo;
    
    @Autowired
    private AttendanceRepository attendanceRepo;

    public ParentController(ParentRepository parentRepo) {
        this.parentRepo = parentRepo;
    }

    @GetMapping
    public List<Parent> getAllParents() {
        return parentRepo.findAll();
    }

    @PostMapping
    public ResponseEntity<?> registerParent(@RequestBody ParentSignupRequest request) {
        if (request.getParentsPw() == null || request.getParentsPw().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("비밀번호는 필수 입력 항목입니다.");
        }

        String encodedPw = passwordEncoder.encode(request.getParentsPw());
        Parent parent = request.toEntity(encodedPw);  
        Parent saved = parentRepo.save(parent);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public Parent getById(@PathVariable String id) {
        return parentRepo.findByParentsId(id);
    }

    // ✅ 자녀 조회 API
    @GetMapping("/{parentId}/children")
    public ResponseEntity<?> getChildren(@PathVariable String parentId) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) return ResponseEntity.notFound().build();

        // 전화번호 대신 Parents_Number 사용
        String parentNumber = parent.getParentsNumber();  
        List<Student> children = studentRepo.findByParentsNumber(parentNumber);

        return ResponseEntity.ok(children);
    }
    
 // ✅ 자녀의 수업 목록 조회
    @GetMapping("/{parentId}/children/{studentId}/classes")
    public ResponseEntity<?> getChildClasses(
            @PathVariable String parentId,
            @PathVariable String studentId) {

        // 자녀가 수강 중인 수업 조회
        List<Course> classes = classRepo.findByStudentsContaining(studentId);
        return ResponseEntity.ok(classes);
    }

    // ✅ 자녀의 수업 출석 내역 조회
    @GetMapping("/{parentId}/children/{studentId}/attendance")
    public ResponseEntity<?> getChildAttendance(
            @PathVariable String parentId,
            @PathVariable String studentId,
            @RequestParam String classId) {

        // 자녀의 해당 수업 출석 내역 조회
        List<Attendance> attendances = attendanceRepo
                .findByClassIdAndAttendedStudentsContaining(classId, studentId);

        return ResponseEntity.ok(attendances);
    }

}
