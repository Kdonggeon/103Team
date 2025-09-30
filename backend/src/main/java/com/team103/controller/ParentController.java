package com.team103.controller;

import com.team103.dto.AddChildrenRequest;
import com.team103.dto.FindIdRequest;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private CourseRepository courseRepo;

    @Autowired
    private AttendanceRepository attendanceRepo;

    public ParentController(ParentRepository parentRepo) {
        this.parentRepo = parentRepo;
    }

    /** 학부모 전체 조회 */
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

    /** 학부모 회원가입 */
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

    /** 학부모 ID로 단건 조회 */
    @GetMapping("/{id}")
    public Parent getById(@PathVariable String id) {
        return parentRepo.findByParentsId(id);
    }

    /** 자녀(여러 명) 추가 */
    @PostMapping("/{parentId}/children")
    public ResponseEntity<?> addChildren(@PathVariable String parentId,
                                         @RequestBody AddChildrenRequest request) {
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

    /** 특정 자녀의 수업 목록 조회 */
    @GetMapping("/{parentId}/children/{studentId}/classes")
    public ResponseEntity<?> getChildClasses(@PathVariable String parentId,
                                             @PathVariable String studentId) {
        List<Course> classes = courseRepo.findByStudentsContaining(studentId);
        return ResponseEntity.ok(classes);
    }

    /** 학부모의 모든 자녀 출석 내역 조회 */
    @GetMapping("/{parentId}/attendance")
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

    /** 학부모의 자녀 목록 조회 */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<?> getChildren(@PathVariable String parentId) {
        return ResponseEntity.ok(findChildrenInternal(parentId));
    }

    /** 별칭 엔드포인트 (동일 로직) */
    @GetMapping("/{parentId}/students")
    public ResponseEntity<?> getChildrenAlias(@PathVariable String parentId) {
        return ResponseEntity.ok(findChildrenInternal(parentId));
    }

    /** 공통 로직: studentIds 우선 → 없으면 parentsNumber 대체 */
    private List<Student> findChildrenInternal(String parentId) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) return new ArrayList<>();

        // 1) studentIds 우선
        List<String> studentIds = parent.getStudentIds();
        if (studentIds != null && !studentIds.isEmpty()) {
            List<Student> s = studentRepo.findByStudentIdIn(studentIds);
            if (s != null && !s.isEmpty()) return s;
        }

        // 2) 없거나 비면 parentsNumber 대체
        String parentNumber = parent.getParentsNumber(); // 타입이 String이면 그대로
        if (parentNumber != null && !parentNumber.isBlank()) {
            List<Student> s = studentRepo.findByParentsNumber(parentNumber);
            if (s != null) return s;
        }
        return new ArrayList<>();
    }

    /** 학부모의 자녀 이름 목록 조회 (보강: studentIds → parentsNumber 폴백) */
    @GetMapping("/{parentId}/children/names")
    public ResponseEntity<?> getChildNames(@PathVariable String parentId) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("부모 정보를 찾을 수 없습니다.");
        }

        List<Student> students = new ArrayList<>();

        // 1) studentIds 우선
        List<String> studentIds = parent.getStudentIds();
        if (studentIds != null && !studentIds.isEmpty()) {
            students = studentRepo.findByStudentIdIn(studentIds);
        }

        // 2) 없거나 비면 parentsNumber 대체
        if (students == null || students.isEmpty()) {
            String parentNumber = parent.getParentsNumber();
            if (parentNumber != null && !parentNumber.isBlank()) {
                students = studentRepo.findByParentsNumber(parentNumber);
            }
        }

        List<String> studentNames = new ArrayList<>();
        if (students != null) {
            for (Student s : students) {
                studentNames.add(s.getStudentName());
            }
        }

        return ResponseEntity.ok(studentNames);
    }

    /** 아이디 찾기 (이름 + 전화번호) */
    @PostMapping("/find_id")
    public ResponseEntity<Map<String,String>> findParentId(@RequestBody FindIdRequest req) {
        String phone = req.normalizedPhone();
        Parent p = parentRepo.findByParentsNameAndParentsPhoneNumber(req.getName(), phone);
        if (p == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        return ResponseEntity.ok(Map.of("username", p.getParentsId()));
    }
}
