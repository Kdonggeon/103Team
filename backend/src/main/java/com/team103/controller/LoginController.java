package com.team103.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.team103.dto.LoginRequest;
import com.team103.dto.LoginResponse;
import com.team103.model.Parent;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.ParentRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import com.team103.security.JwtUtil;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private StudentRepository studentRepo;
    @Autowired
    private TeacherRepository teacherRepo;
    @Autowired
    private ParentRepository parentRepo;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        // 1. 학생 체크
        Student student = studentRepo.findByStudentIdAndStudentPw(
                Long.parseLong(request.getUsername()), request.getPassword());
        if (student != null) {
            String token = jwtUtil.generateToken(student.getUsername(), "student");
            return ResponseEntity.ok(
                    new LoginResponse("success", "student", student.getUsername(), student.getName(), token));
        }

        // 2. 교사 체크
        Teacher teacher = teacherRepo.findByUsernameAndPassword(request.getUsername(), request.getPassword());
        if (teacher != null) {
            String token = jwtUtil.generateToken(teacher.getUsername(), "teacher");
            return ResponseEntity.ok(
                    new LoginResponse("success", "teacher", teacher.getUsername(), teacher.getName(), token));
        }

        // 3. 학부모 체크
        Parent parent = parentRepo.findByUsernameAndPassword(request.getUsername(), request.getPassword());
        if (parent != null) {
            String token = jwtUtil.generateToken(parent.getUsername(), "parent");
            return ResponseEntity.ok(
                    new LoginResponse("success", "parent", parent.getUsername(), parent.getName(), token));
        }

        // 4. 로그인 실패
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "fail"));
    }
}
