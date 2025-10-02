package com.team103.controller;

import com.team103.dto.TeacherUpdateRequest;
import com.team103.model.Teacher;
import com.team103.repository.TeacherRepository;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/teachers")
public class TeacherUpdateController {

    private final TeacherRepository teacherRepository;

    public TeacherUpdateController(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTeacher(@PathVariable String id,
                                           @RequestBody TeacherUpdateRequest request,
                                           Authentication auth) {
        // 1) 인증 사용자 확인
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "UNAUTHENTICATED"));
        }
        // 2) 권한: 본인 또는(옵션) 원장/관리자
        boolean isSelf = id.equals(auth.getName());
        boolean isDirector = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DIRECTOR".equals(a.getAuthority()));
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())); // 있으면 사용

        if (!(isSelf || isDirector || isAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "NO_PERMISSION"));
        }

        // 3) 대상 조회
        Teacher teacher = teacherRepository.findByTeacherId(id);
        if (teacher == null) {
            return ResponseEntity.notFound().build();
        }

        // 4) 부분 업데이트(널만 아닌 값 반영)
        if (request.getTeacherName() != null) {
            teacher.setTeacherName(request.getTeacherName());
        }
        if (request.getTeacherPhoneNumber() != null) {
            teacher.setTeacherPhoneNumber(request.getTeacherPhoneNumber());
        }
        // academyNumber가 양수일 때만 반영 (0/음수는 무시)
        if (request.getAcademyNumber() > 0) {
            teacher.setAcademyNumbers(Collections.singletonList(request.getAcademyNumber()));
        }

        teacherRepository.save(teacher);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
