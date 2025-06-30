package com.team103.controller;

import com.team103.dto.TeacherSignupRequest;
import com.team103.model.Teacher;
import com.team103.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/signup/teacher")
public class TeacherSignupController {

    @Autowired
    private TeacherRepository teacherRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<String> signup(@RequestBody TeacherSignupRequest req) {
        if (teacherRepo.existsByTeacherId(req.getTeacherId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 교사 ID입니다");
        }

        String encryptedPw;
        try {
            encryptedPw = passwordEncoder.encode(req.getTeacherPw());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호 암호화 실패");
        }

        Teacher teacher = req.toEntity(encryptedPw);
        teacherRepo.save(teacher);

        return ResponseEntity.ok("교사 회원가입 성공");
    }
}
