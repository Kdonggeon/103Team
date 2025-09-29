package com.team103.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team103.dto.StudentSignupRequest;
import com.team103.model.Student;
import com.team103.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/signup/student")
public class StudentSignupController {

    @Autowired private StudentRepository studentRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> signup(@RequestBody StudentSignupRequest req) {

        // 1) 필수값 검증 (리액트/모바일 모두 커버)
        if (isBlank(req.getStudentId()) || isBlank(req.getStudentPw()) || isBlank(req.getStudentName())) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "fail",
                "message", "필수 항목(studentId, studentPw, studentName)이 비어있습니다."
            ));
        }

        // 2) 중복 검사: 로그인 아이디 기준
        if (studentRepo.existsByStudentId(req.getStudentId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", "fail",
                "message", "이미 존재하는 학생 아이디입니다"
            ));
        }

        try {
            // 3) 비밀번호 암호화
            String enc = passwordEncoder.encode(req.getStudentPw());

            // 4) 엔티티 생성 & 저장
            Student student = req.toEntity(enc);
            studentRepo.save(student);

            // 5) 성공 응답
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "학생 회원가입 성공",
                "studentId", student.getStudentId()
            ));
        } catch (IllegalArgumentException e) {
            // e.g. 암호화 null 등
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "fail",
                "message", "비밀번호 암호화 실패"
            ));
        } catch (Exception e) {
            // 기타 예외 로깅
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "서버 오류가 발생했습니다"
            ));
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
