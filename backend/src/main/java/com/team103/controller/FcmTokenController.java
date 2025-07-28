package com.team103.controller;

import com.team103.dto.FcmTokenRequest;
import com.team103.model.Student;
import com.team103.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/fcm")
public class FcmTokenController {

    private final StudentRepository studentRepository;

    @Autowired
    public FcmTokenController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @PostMapping("/registerToken")
    public ResponseEntity<String> registerToken(@RequestBody FcmTokenRequest request) {
        if (request.getUserId() == null || request.getToken() == null) {
            return ResponseEntity.badRequest().body("userId와 token을 모두 입력해야 합니다.");
        }

        Optional<Student> studentOpt = studentRepository.findById(request.getUserId());
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            student.setFcmToken(request.getToken());
            studentRepository.save(student);
            return ResponseEntity.ok("토큰 저장 성공");
        } else {
            return ResponseEntity.status(404).body("학생을 찾을 수 없습니다.");
        }
    }
}
