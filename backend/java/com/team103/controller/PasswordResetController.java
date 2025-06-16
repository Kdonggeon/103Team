package com.team103.controller;

import com.team103.dto.PasswordResetRequest;
import com.team103.model.Parent;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.ParentRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reset-password")
public class PasswordResetController {

    @Autowired private StudentRepository studentRepo;
    @Autowired private TeacherRepository teacherRepo;
    @Autowired private ParentRepository parentRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest req) {
        String role = req.getRole();
        String id = req.getId();
        String name = req.getName();
        String phone = req.getPhone();
        String newPw = req.getNewPassword();

        if (newPw == null || newPw.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("새 비밀번호는 필수입니다");
        }

        switch (role) {
            case "student":
                Student student = studentRepo.findByStudentId(id);
                if (student != null &&
                        student.getStudentName().equals(name) &&
                        student.getStudentPhoneNumber().equals(phone)) {

                    student.setStudentPw(passwordEncoder.encode(newPw));
                    studentRepo.save(student);
                    return ResponseEntity.ok("비밀번호 변경 완료 (학생)");
                }
                break;

            case "teacher":
                Teacher teacher = teacherRepo.findByTeacherId(id);
                if (teacher != null &&
                        teacher.getTeacherName().equals(name) &&
                        String.valueOf(teacher.getTeacherPhoneNumber()).equals(phone)) {

                    teacher.setTeacherPw(passwordEncoder.encode(newPw));
                    teacherRepo.save(teacher);
                    return ResponseEntity.ok("비밀번호 변경 완료 (교사)");
                }
                break;

            case "parent":
                Parent parent = parentRepo.findByParentsId(id);
                if (parent != null &&
                        parent.getParentsName().equals(name) &&
                        String.valueOf(parent.getParentsPhoneNumber()).equals(phone)) {

                    parent.setParentsPw(passwordEncoder.encode(newPw));
                    parentRepo.save(parent);
                    return ResponseEntity.ok("비밀번호 변경 완료 (학부모)");
                }
                break;
        }

        return ResponseEntity.badRequest().body("정보가 일치하지 않습니다");
    }
}
