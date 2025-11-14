package com.team103.controller;

import com.team103.dto.StudentUpdateRequest;
import com.team103.model.Student;
import com.team103.repository.StudentRepository;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentUpdateController {

    private final StudentRepository studentRepository;

    public StudentUpdateController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(@PathVariable String id,
                                           @RequestBody StudentUpdateRequest req,
                                           Authentication auth) {

        // -------------------------------------
        // 🔐 1) 로그인 여부 확인
        // -------------------------------------
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "UNAUTHENTICATED"));
        }

        String loginUser = auth.getName();   // JWT에 들어 있는 username
        String role = auth.getAuthorities().iterator().next().getAuthority(); // 예: ROLE_parent, ROLE_student


        // -------------------------------------
        // 🔎 2) 수정할 학생 조회
        // -------------------------------------
        Student s = studentRepository.findByStudentId(id);
        if (s == null) {
            return ResponseEntity.notFound().build();
        }


        // -------------------------------------
        // 🔥 3) 권한 체크
        // -------------------------------------

        // (A) 학생 본인인지?
        boolean isStudentSelf = id.equals(loginUser);

        // (B) 로그인한 사용자가 부모 권한인지?
        boolean isParentRole =
                role.equalsIgnoreCase("parent") ||
                role.equalsIgnoreCase("ROLE_parent");

        // (C) 부모가 처음 접근한 경우 → parentId 자동 연결
        if (isParentRole && s.getParentId() == null) {
            s.setParentId(loginUser);
            studentRepository.save(s);
        }

        // (D) 부모가 이 학생의 부모인지?
        boolean isParentOfThisStudent =
                isParentRole &&
                s.getParentId() != null &&
                s.getParentId().equals(loginUser);

        // (E) 권한 없으면 403
        if (!isStudentSelf && !isParentOfThisStudent) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "NO_PERMISSION"));
        }


        // -------------------------------------
        // ✏️ 4) 실제 업데이트 (null 무시)
        // -------------------------------------

        if (req.getStudentName() != null)
            s.setStudentName(req.getStudentName());

        if (req.getStudentPhoneNumber() != null)
            s.setStudentPhoneNumber(req.getStudentPhoneNumber());

        if (req.getAddress() != null)
            s.setAddress(req.getAddress());

        if (req.getSchool() != null)
            s.setSchool(req.getSchool());

        if (req.getGender() != null)
            s.setGender(req.getGender());

        if (req.getGrade() != null)
            s.setGrade(req.getGrade());

        // 부모 정보도 업데이트 가능
        if (req.getParentId() != null)
            s.setParentId(req.getParentId());

        if (req.getParentsNumber() != null)
            s.setParentsNumber(req.getParentsNumber());


        // -------------------------------------
        // 💾 5) DB 저장
        // -------------------------------------
        studentRepository.save(s);

        // -------------------------------------
        // ✅ 6) 응답
        // -------------------------------------
        return ResponseEntity.ok(Map.of("message", "SUCCESS"));
    }
}
