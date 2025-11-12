package com.team103.controller;

import com.team103.dto.StudentUpdateRequest;
import com.team103.model.Student;
import com.team103.repository.StudentRepository;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentUpdateController {

    private final StudentRepository studentRepository;

    public StudentUpdateController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(@PathVariable String id,
                                           @RequestBody StudentUpdateRequest req,
                                           Authentication auth) {
        // ✅ 로그인 사용자 검증 (JWT 필터에서 넣어준 username)
        if (auth == null || !id.equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "NO_PERMISSION"));
        }

        Student s = studentRepository.findByStudentId(id);
        if (s == null) return ResponseEntity.notFound().build();

        // ✅ null 아닌 필드만 반영 (부분 업데이트 안전)
        if (req.getStudentName() != null)        s.setStudentName(req.getStudentName());
        if (req.getStudentPhoneNumber() != null) s.setStudentPhoneNumber(req.getStudentPhoneNumber());
        if (req.getAddress() != null)            s.setAddress(req.getAddress());
        if (req.getSchool() != null)             s.setSchool(req.getSchool());
        if (req.getGender() != null)             s.setGender(req.getGender());
        if (req.getGrade() != 0)                 s.setGrade(req.getGrade()); // 0이 유효값이면 별도 처리

        studentRepository.save(s);
        return ResponseEntity.ok(Map.of("status","ok"));
    }
}  