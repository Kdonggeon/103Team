package com.team103.controller;

import com.team103.dto.ParentSignupRequest;
import com.team103.model.Parent;
import com.team103.repository.ParentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/signup/parent")
public class ParentSignupController {

    @Autowired
    private ParentRepository parentRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<String> signup(@RequestBody ParentSignupRequest req) {

        // 1. username 중복 확인
        if (parentRepo.existsByUsername(req.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 학부모 ID입니다");
        }

        // 2. 비밀번호 암호화
        String encodedPw;
        try {
            encodedPw = passwordEncoder.encode(req.getPassword());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호 암호화 실패");
        }

        // 3. Parent 객체 생성 후 저장
        Parent parent = req.toEntity(encodedPw);
        parentRepo.save(parent);

        return ResponseEntity.ok("학부모 회원가입 성공");
    }
}
