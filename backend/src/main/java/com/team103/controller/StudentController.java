package com.team103.controller;

import com.team103.dto.FindIdRequest;
import com.team103.dto.StudentSignupRequest;
import com.team103.model.Student;
import com.team103.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public StudentController(StudentRepository repo) {
        this.repo = repo;
    }

    /** 전체 학생 조회 */
    @GetMapping
    public List<Student> getAllStudents() {
        return repo.findAll();
    }

    /** 학생 회원가입 */
    @PostMapping
    public ResponseEntity<?> createStudent(@RequestBody StudentSignupRequest request) {
        // 1) 비밀번호 필수 체크
        String rawPw = request.getStudentPw();
        if (rawPw == null || rawPw.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("비밀번호는 필수 입력 항목입니다.");
        }

        // (선택) 아이디 중복 체크
        // if (repo.existsByStudentId(request.getStudentId())) {
        //     return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 아이디입니다.");
        // }

        // 2) 암호화
        String encodedPw = passwordEncoder.encode(rawPw);

        // 3) DTO → Entity (암호화된 PW 주입)
        Student student = request.toEntity(encodedPw);

        // 4) 저장
        Student saved = repo.save(student);
        return ResponseEntity.ok(saved);
    }

    /** studentId 기준 조회 */
    @GetMapping("/{id}")
    public Student getStudentByStudentId(@PathVariable("id") String studentId) {
        return repo.findByStudentId(studentId);
    }

    /** FCM 토큰 업데이트 */
    @PutMapping("/{id}/fcm-token")
    public ResponseEntity<Void> updateFcmToken(@PathVariable("id") String studentId,
                                               @RequestParam("token") String token) {
        Student student = repo.findByStudentId(studentId);
        if (student == null) {
            return ResponseEntity.notFound().build();
        }
        student.setFcmToken(token);
        repo.save(student);
        return ResponseEntity.ok().build();
    }

    /** 아이디 찾기 (이름+전화번호) */
    @PostMapping("/find_id")
    public ResponseEntity<Map<String, String>> findStudentId(@RequestBody FindIdRequest req) {
        String phone = req.normalizedPhone(); // 하이픈/공백 제거
        Student s = repo.findByStudentNameAndStudentPhoneNumber(req.getName(), phone);
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(Map.of("username", s.getStudentId()));
    }
}
