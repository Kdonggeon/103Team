package com.team103.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired private StudentRepository studentRepo;
    @Autowired private TeacherRepository teacherRepo;
    @Autowired private ParentRepository parentRepo;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // 1. 학생
        Student student = studentRepo.findByStudentId(username);
        if (student != null && passwordEncoder.matches(password, student.getStudentPw())) {
            String token = jwtUtil.generateToken(student.getStudentId(), "student");
            return ResponseEntity.ok(new LoginResponse("success", "student",
                    student.getStudentId(), student.getStudentName(), token));
        }

     // 2. 교사
        Teacher teacher = teacherRepo.findByTeacherId(username);
        if (teacher != null && passwordEncoder.matches(password, teacher.getTeacherPw())) {
            String token = jwtUtil.generateToken(teacher.getTeacherId(), "teacher");
            return ResponseEntity.ok(new LoginResponse("success", "teacher",
                    teacher.getTeacherId(), teacher.getTeacherName(), token));
        }

        // 3. 학부모
        Parent parent = parentRepo.findByParentsId(username);
        if (parent != null && passwordEncoder.matches(password, parent.getParentsPw())) {
            String token = jwtUtil.generateToken(parent.getParentsId(), "parent");
            return ResponseEntity.ok(new LoginResponse("success", "parent",
                    parent.getParentsId(), parent.getParentsName(), token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "fail", "message", "일치하는 계정이 없습니다"));
    }
}
