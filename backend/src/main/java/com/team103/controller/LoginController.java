package com.team103.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team103.dto.LoginRequest;
import com.team103.dto.LoginResponse;
import com.team103.model.Parent;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.ParentRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import com.team103.security.JwtUtil;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired private StudentRepository studentRepo;
    @Autowired private TeacherRepository teacherRepo;
    @Autowired private ParentRepository parentRepo;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private HttpSession session;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // 1. 학생 로그인 처리
        Student student = studentRepo.findByStudentId(username);
        if (student != null && passwordEncoder.matches(password, student.getStudentPw())) {

            // FCM 토큰 업데이트
            if (request.getFcmToken() != null && !request.getFcmToken().isEmpty()) {
                student.setFcmToken(request.getFcmToken());
                studentRepo.save(student);
            }

            String token = jwtUtil.generateToken(student.getStudentId(), "student");
            session.setAttribute("username", student.getStudentId());
            session.setAttribute("role", "student");

            // 수정: academyNumbers 필드에 List<Integer> 직접 전달
            LoginResponse res = new LoginResponse(
                "success",
                "student",
                student.getStudentId(),
                student.getStudentName(),
                token,
                student.getStudentPhoneNumber(),
                student.getAddress(),
                student.getSchool(),
                student.getGrade(),
                student.getGender(),
                student.getAcademyNumbers() 
            );

            try {
                System.out.println("학생 로그인 응답 → " + new ObjectMapper().writeValueAsString(res));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok(res);
        }

        // 2. 교사 로그인 처리
        Teacher teacher = teacherRepo.findByTeacherId(username);
        if (teacher != null && passwordEncoder.matches(password, teacher.getTeacherPw())) {
            String token = jwtUtil.generateToken(teacher.getTeacherId(), "teacher");
            session.setAttribute("username", teacher.getTeacherId());
            session.setAttribute("role", "teacher");

            // 수정: academyNumbers 필드에 List<Integer> 직접 전달
            LoginResponse res = new LoginResponse(
                "success",
                "teacher",
                teacher.getTeacherId(),
                teacher.getTeacherName(),
                token,
                teacher.getTeacherPhoneNumber(),
                null,
                null,
                0,
                null,
                teacher.getAcademyNumbers()  // 기존 Collections.singletonList() 제거
            );

            try {
                System.out.println("교사 로그인 응답 → " + new ObjectMapper().writeValueAsString(res));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok(res);
        }

        // 3. 학부모 로그인 처리
     // 3. 학부모 로그인 처리
        Parent parent = parentRepo.findByParentsId(username);
        if (parent != null && passwordEncoder.matches(password, parent.getParentsPw())) {
            String token = jwtUtil.generateToken(parent.getParentsId(), "parent");
            session.setAttribute("username", parent.getParentsId());
            session.setAttribute("role", "parent");

            // ✅ 자녀(Student)의 academyNumbers 수집
            List<Student> children = studentRepo.findByParentsNumber(parent.getParentsNumber());
            Set<Integer> academyNumberSet = new HashSet<>();

            String firstChildId = null;
            if (children != null && !children.isEmpty()) {
                for (Student child : children) {
                    // 각 자녀의 academyNumbers에서 학원 번호 수집
                    if (child.getAcademyNumbers() != null) {
                        academyNumberSet.addAll(child.getAcademyNumbers());
                    }
                }
                firstChildId = children.get(0).getStudentId();  // 첫 번째 자녀 ID 저장
            }

            // ✅ 학원 번호 리스트로 변환
            List<Integer> academyNumbers = new ArrayList<>(academyNumberSet);

            // ✅ 응답 생성 시 academyNumbers 전달
            LoginResponse res = new LoginResponse(
                "success",
                "parent",
                parent.getParentsId(),
                parent.getParentsName(),
                token,
                parent.getParentsPhoneNumber(),
                null,
                null,
                0,
                null,
                academyNumbers
            );

            res.setParentsNumber(parent.getParentsNumber());
            res.setChildStudentId(firstChildId);  // 첫 자녀 ID 설정

            try {
                System.out.println("최종 응답 → " + new ObjectMapper().writeValueAsString(res));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok(res);
        }

        // 로그인 실패
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                             .body(Map.of("status", "fail", "message", "일치하는 계정이 없습니다"));
    }
}
