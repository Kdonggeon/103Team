package com.team103.controller;

import com.team103.dto.StudentUpdateRequest;
import com.team103.model.Student;
import com.team103.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentUpdateController {

    @Autowired
    private StudentRepository studentRepository;

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(@PathVariable String id, @RequestBody StudentUpdateRequest request) {
        Student student = studentRepository.findByStudentId(id);
        if (student == null) {
            return ResponseEntity.notFound().build();
        }

        student.setStudentName(request.getStudentName());
        student.setStudentPhoneNumber(request.getStudentPhoneNumber());
        student.setAddress(request.getAddress());
        student.setSchool(request.getSchool());
        student.setGrade(request.getGrade());
        student.setGender(request.getGender());

        studentRepository.save(student);
        return ResponseEntity.ok("학생 정보 수정 완료");
    }
}  