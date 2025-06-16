package com.team103.controller;

import com.team103.dto.TeacherUpdateRequest;
import com.team103.model.Teacher;
import com.team103.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teachers")
public class TeacherUpdateController {

    @Autowired
    private TeacherRepository teacherRepository;

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTeacher(@PathVariable String id, @RequestBody TeacherUpdateRequest request) {
        Teacher teacher = teacherRepository.findByTeacherId(id);
        if (teacher == null) {
            return ResponseEntity.notFound().build();
        }

        teacher.setTeacherName(request.getTeacherName());
        teacher.setTeacherPhoneNumber(request.getTeacherPhoneNumber());
        teacher.setAcademyNumber(request.getAcademyNumber());

        teacherRepository.save(teacher);
        return ResponseEntity.ok("교사 정보 수정 완료");
    }
}
