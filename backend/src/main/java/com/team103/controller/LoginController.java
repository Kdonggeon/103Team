package com.team103.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    @Autowired private DirectorRepository directorRepo;

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
                teacher.getAcademyNumbers()
            );

            try {
                System.out.println("교사 로그인 응답 → " + new ObjectMapper().writeValueAsString(res));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok(res);
        }

        // 3. 원장 로그인
        Director director = directorRepo.findByUsername(username);

        System.out.println("디버깅: 입력 ID = " + username);
        System.out.println("디버깅: 찾은 원장 = " + (director != null ? director.getUsername() : "없음"));
        System.out.println("디버깅: 저장된 해시 = " + (director != null ? director.getPassword() : "없음"));
        System.out.println("디버깅: 비번 일치 = " + (director != null && passwordEncoder.matches(password, director.getPassword())));

        if (director != null && passwordEncoder.matches(password, director.getPassword())) {
            String token = jwtUtil.generateToken(username, "director");

            LoginResponse response = new LoginResponse(
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
            return ResponseEntity.ok(response);
        }

        // 4. 학부모 로그인 처리
        Parent parent = parentRepo.findByParentsId(username);
        if (parent != null && passwordEncoder.matches(password, parent.getParentsPw())) {
            String token = jwtUtil.generateToken(parent.getParentsId(), "parent");
            session.setAttribute("username", parent.getParentsId());
            session.setAttribute("role", "parent");

            // ✅ 학원 번호 수집: ① Parent 자체 → ② studentIds → ③ Parents_Number
            Set<Integer> academySet = new LinkedHashSet<>();

            // ① Parent 문서 자체의 Academy_Numbers 사용 (있다면)
            try {
                List<Integer> pAcademies = parent.getAcademyNumbers();
                if (pAcademies != null && !pAcademies.isEmpty()) {
                    academySet.addAll(pAcademies);
                    System.out.println("학부모 보완① parent.academyNumbers 사용: " + pAcademies);
                }
            } catch (NoSuchMethodError | Exception ignore) {
                // 모델에 필드가 없을 수도 있음
            }

            // ② Parent가 보유한 studentIds로 학생 일괄 조회 (레포에 메서드가 있어야 함)
            boolean step2Tried = false;
            try {
                List<String> sids = parent.getStudentIds();
                if ((sids != null && !sids.isEmpty()) && academySet.isEmpty()) {
                    step2Tried = true;
                    List<Student> childrenByIds = studentRepo.findByStudentIdIn(sids);
                    if (childrenByIds != null) {
                        for (Student s : childrenByIds) {
                            if (s != null && s.getAcademyNumbers() != null) {
                                academySet.addAll(s.getAcademyNumbers());
                            }
                        }
                    }
                    System.out.println("학부모 보완② studentIds 기반 수집: size=" + academySet.size());
                }
            } catch (NoSuchMethodError | Exception e) {
                // findByStudentIdIn 또는 getStudentIds가 없을 수 있음
                if (step2Tried) e.printStackTrace();
            }

            // ③ Parents_Number 키로 자녀 조회 (기존 로직)
            if (academySet.isEmpty()) {
                String pno = parent.getParentsNumber();
                List<Student> children = (pno == null || pno.isEmpty())
                        ? new ArrayList<>()
                        : studentRepo.findByParentsNumber(pno);

                String firstChildId = null;
                if (children != null && !children.isEmpty()) {
                    for (Student child : children) {
                        if (child.getAcademyNumbers() != null) {
                            academySet.addAll(child.getAcademyNumbers());
                        }
                    }
                    firstChildId = children.get(0).getStudentId();
                }

                List<Integer> academyNumbers = new ArrayList<>(academySet);

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
                res.setChildStudentId(firstChildId);

                try {
                    System.out.println("학부모 로그인 응답(③ 포함) → " + new ObjectMapper().writeValueAsString(res));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return ResponseEntity.ok(res);
            } else {
                // ① 또는 ②에서 이미 academySet을 채운 경우
                List<Integer> academyNumbers = new ArrayList<>(academySet);

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
                // firstChildId는 ①/② 경로에서는 확정 불가 → 필요 시 클라이언트에서 최초 자녀 조회

                try {
                    System.out.println("학부모 로그인 응답(①/② 경로) → " + new ObjectMapper().writeValueAsString(res));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return ResponseEntity.ok(res);
            }
        }

        // 로그인 실패
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                             .body(Map.of("status", "fail", "message", "일치하는 계정이 없습니다"));
    }
}
