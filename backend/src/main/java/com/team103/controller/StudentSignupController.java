package com.team103.controller;

import com.team103.dto.StudentSignupRequest;
import com.team103.model.Student;
import com.team103.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/signup/student")
public class StudentSignupController {

    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<String> signup(@RequestBody StudentSignupRequest req) {
        if (studentRepo.existsById(req.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 학생 ID입니다");
        }

        // 비밀번호 암호화 (String 타입으로 저장)
        String encryptedPw;
        try {
            encryptedPw = passwordEncoder.encode(req.getStudentPw());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호 암호화 실패");
        }

        // Student 객체 생성
        Student student = new Student(
        	    req.getId(),
        	    null,                      
        	    req.getStudentName(),
        	    req.getStudentId(),
        	    encryptedPw,
        	    req.getAddress(),
        	    req.getPhoneNumber(),
        	    req.getSchool(),
        	    req.getGrade(),
        	    req.getParentsNumber(),
        	    req.getSeatNumber(),
        	    req.isCheckedIn(),
        	    req.getGender()
        	);

        studentRepo.save(student);
        return ResponseEntity.ok("학생 회원가입 성공");
    }
}

