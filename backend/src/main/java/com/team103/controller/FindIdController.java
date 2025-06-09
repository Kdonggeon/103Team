package com.team103.controller;

import com.team103.dto.FindIdRequest;
import com.team103.dto.FindIdResponse;
import com.team103.model.Student;
import com.team103.model.Parent;
import com.team103.model.Teacher;
import com.team103.repository.StudentRepository;
import com.team103.repository.ParentRepository;
import com.team103.repository.TeacherRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FindIdController {

    @Autowired private StudentRepository studentRepo;
    @Autowired private ParentRepository parentRepo;
    @Autowired private TeacherRepository teacherRepo;

    @PostMapping("/find-id")
    public ResponseEntity<?> findId(@RequestBody FindIdRequest request) {
        String role = request.getRole();
        String name = request.getName();
        String phone = request.getPhone();

        switch (role.toLowerCase()) {
            case "student":
                Student s = studentRepo.findByStudentNameAndStudentPhoneNumber(name, phone);
                if (s != null) {
                    return ResponseEntity.ok(new FindIdResponse("student", s.getStudentId()));
                }
                return ResponseEntity.status(404).body("해당 학생 정보를 찾을 수 없습니다");

            case "parent":
                Parent p = parentRepo.findByParentsNameAndParentsPhoneNumber(name, phone);
                if (p != null) {
                    return ResponseEntity.ok(new FindIdResponse("parent", p.getParentsId()));
                }
                return ResponseEntity.status(404).body("해당 학부모 정보를 찾을 수 없습니다");

            case "teacher":
                Teacher t = teacherRepo.findByTeacherNameAndTeacherPhoneNumber(name, phone);
                if (t != null) {
                    return ResponseEntity.ok(new FindIdResponse("teacher", t.getTeacherId()));
                }
                return ResponseEntity.status(404).body("해당 교사 정보를 찾을 수 없습니다");

            default:
                return ResponseEntity.badRequest().body("역할(role) 값이 잘못되었습니다. (student, parent, teacher 중 하나)");
        }
    }
}
