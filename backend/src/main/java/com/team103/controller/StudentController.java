package com.team103.controller;

import com.team103.dto.StudentSignupRequest;
import com.team103.model.Student;
import com.team103.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public List<Student> getAllStudents() {
        return repo.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createStudent(@RequestBody StudentSignupRequest request) {
        // ✅ 1. 비밀번호 null 또는 빈 문자열 체크
        if (request.getStudentPw() == null || request.getStudentPw().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("비밀번호는 필수 입력 항목입니다.");
        }

        // ✅ 2. DTO → Entity 변환
        Student student = request.toEntity();

        // ✅ 3. 비밀번호 암호화
        String encodedPw = passwordEncoder.encode(request.getStudentPw());
        student.setStudentPw(encodedPw);

        // ✅ 4. 저장
        Student saved = repo.save(student);

        return ResponseEntity.ok(saved);
    }


    @GetMapping("/{id}")
    public Student getStudentByStudentId(@PathVariable("id") String studentId) {
        return repo.findByStudentId(studentId);
    }
}
