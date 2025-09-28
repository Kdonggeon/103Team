package com.team103.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team103.dto.LoginRequest;
import com.team103.dto.LoginResponse;
import com.team103.model.Director;
import com.team103.model.Parent;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.DirectorRepository;
import com.team103.repository.ParentRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import com.team103.security.JwtUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired private StudentRepository studentRepo;
    @Autowired private TeacherRepository teacherRepo;
    @Autowired private ParentRepository parentRepo;
    @Autowired private DirectorRepository directorRepo;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private HttpSession session;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // 1) 학생
        Student student = studentRepo.findByStudentId(username);
        if (student != null && safeMatches(password, student.getStudentPw())) {

            // FCM 토큰 업데이트 (옵션)
            if (request.getFcmToken() != null && !request.getFcmToken().isEmpty()) {
                student.setFcmToken(request.getFcmToken());
                studentRepo.save(student);
            }

            String token = jwtUtil.generateToken(student.getStudentId(), "student");
            session.setAttribute("username", student.getStudentId());
            session.setAttribute("role", "student");

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

            logJson("학생 로그인 응답", res);
            return ResponseEntity.ok(res);
        }

        // 2) 교사
        Teacher teacher = teacherRepo.findByTeacherId(username);
        if (teacher != null && safeMatches(password, teacher.getTeacherPw())) {
            String token = jwtUtil.generateToken(teacher.getTeacherId(), "teacher");
            session.setAttribute("username", teacher.getTeacherId());
            session.setAttribute("role", "teacher");

            LoginResponse res = new LoginResponse(
                "success",
                "teacher",
                teacher.getTeacherId(),
                teacher.getTeacherName(),
                token,
                teacher.getTeacherPhoneNumber(),
                null, null, 0, null,
                teacher.getAcademyNumbers()
            );

            logJson("교사 로그인 응답", res);
            return ResponseEntity.ok(res);
        }

        // 3) 원장
        Director director = directorRepo.findByUsername(username);
        if (director != null) {
            boolean pwOk = false;
            try {
                pwOk = passwordEncoder.matches(password, director.getPassword());
            } catch (Exception ignore) {
                // 저장된 비번이 bcrypt가 아닐 경우 대비 (평문 비교 + 마이그레이션)
                if (director.getPassword() != null && director.getPassword().equals(password)) {
                    pwOk = true;
                    director.setPassword(passwordEncoder.encode(password));
                    directorRepo.save(director);
                }
            }

            if (pwOk) {
                String token = jwtUtil.generateToken(director.getUsername(), "director");

                LoginResponse res = new LoginResponse(
                    "success",
                    "director",
                    director.getUsername(),
                    director.getName(),
                    token,
                    director.getPhone(),
                    null, null, 0, null,
                    director.getAcademyNumbers()
                );

                session.setAttribute("username", director.getUsername());
                session.setAttribute("role", "director");

                logJson("원장 로그인 응답", res);
                return ResponseEntity.ok(res);
            }
        }

        // 4) 학부모
        Parent parent = parentRepo.findByParentsId(username);
        if (parent != null && safeMatches(password, parent.getParentsPw())) {
            String token = jwtUtil.generateToken(parent.getParentsId(), "parent");
            session.setAttribute("username", parent.getParentsId());
            session.setAttribute("role", "parent");

            List<Student> children = studentRepo.findByParentsNumber(parent.getParentsNumber());
            Set<Integer> academyNumberSet = new HashSet<>();
            String firstChildId = null;

            if (children != null && !children.isEmpty()) {
                for (Student child : children) {
                    if (child.getAcademyNumbers() != null) {
                        academyNumberSet.addAll(child.getAcademyNumbers());
                    }
                }
                firstChildId = children.get(0).getStudentId();
            }

            List<Integer> academyNumbers = new ArrayList<>(academyNumberSet);

            LoginResponse res = new LoginResponse(
                "success",
                "parent",
                parent.getParentsId(),
                parent.getParentsName(),
                token,
                parent.getParentsPhoneNumber(),
                null, null, 0, null,
                academyNumbers
            );
            res.setParentsNumber(parent.getParentsNumber());
            res.setChildStudentId(firstChildId);

            logJson("학부모 로그인 응답", res);
            return ResponseEntity.ok(res);
        }

        // 실패
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "fail", "message", "일치하는 계정이 없습니다"));
    }

    /** bcrypt null/예외 방지용 안전 매칭 */
    private boolean safeMatches(String raw, String encoded) {
        try {
            return encoded != null && passwordEncoder.matches(raw, encoded);
        } catch (Exception e) {
            return false;
        }
    }

    private void logJson(String prefix, Object obj) {
        try {
            System.out.println(prefix + " → " + new ObjectMapper().writeValueAsString(obj));
        } catch (Exception ignored) {}
    }
}
