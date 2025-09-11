package com.team103.controller;

import com.team103.dto.AddChildrenRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    
    @PutMapping("/{id}/fcm-token")
    public ResponseEntity<Void> updateFcmToken(@PathVariable("id") String parentsId,
                                               @RequestParam("token") String token) {
        Parent p = parentRepo.findByParentsId(parentsId);
        if (p == null) return ResponseEntity.notFound().build();
        p.setFcmToken((token == null || token.isBlank()) ? null : token);
        parentRepo.save(p);
        return ResponseEntity.ok().build();
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

    @PostMapping("/{parentId}/children")
    public ResponseEntity<?> addChildren(@PathVariable String parentId, @RequestBody AddChildrenRequest request) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("부모 정보를 찾을 수 없습니다.");
        }

        List<String> currentChildren = parent.getStudentIds();
        if (currentChildren == null) currentChildren = new ArrayList<>();

        for (String studentId : request.getStudentIds()) {
            if (!currentChildren.contains(studentId)) {
                currentChildren.add(studentId);
            }
        }

        parent.setStudentIds(currentChildren);
        parentRepo.save(parent);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{parentId}/children/{studentId}/classes")
    public ResponseEntity<?> getChildClasses(@PathVariable String parentId, @PathVariable String studentId) {
        List<Course> classes = classRepo.findByStudentsContaining(studentId);
        return ResponseEntity.ok(classes);
    }

    @GetMapping("/parents/{parentId}/attendance")
    public ResponseEntity<?> getChildAttendance(@PathVariable String parentId) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("부모님 정보 없음");
        }

        String parentNumber = parent.getParentsNumber();
        List<Student> children = studentRepo.findByParentsNumber(parentNumber);

        List<Attendance> allAttendance = new ArrayList<>();
        for (Student child : children) {
            List<Attendance> attendance = attendanceRepo.findByStudentInAttendanceList(child.getStudentId());
            allAttendance.addAll(attendance);
        }

        return ResponseEntity.ok(allAttendance);
    }

    @GetMapping("/{parentId}/children")
    public ResponseEntity<?> getChildren(@PathVariable String parentId) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) return ResponseEntity.notFound().build();

        String parentNumber = parent.getParentsNumber();
        List<Student> children = studentRepo.findByParentsNumber(parentNumber);

        return ResponseEntity.ok(children);
    }

    @GetMapping("/{parentId}/children/names")
    public ResponseEntity<?> getChildNames(@PathVariable String parentId) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("부모 정보를 찾을 수 없습니다.");
        }

        List<String> studentIds = parent.getStudentIds();
        if (studentIds == null || studentIds.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<Student> students = studentRepo.findByStudentIdIn(studentIds);
        List<String> studentNames = students.stream()
                .map(Student::getStudentName)
                .toList();

        return ResponseEntity.ok(studentNames);
    }
}
