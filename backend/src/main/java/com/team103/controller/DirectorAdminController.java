// src/main/java/com/team103/controller/DirectorAdminController.java
package com.team103.controller;

import com.team103.dto.StudentLiteDto;
import com.team103.dto.TeacherLiteDto;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/directors")
@CrossOrigin(origins = "*")
public class DirectorAdminController {

    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;

    public DirectorAdminController(StudentRepository studentRepo, TeacherRepository teacherRepo) {
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
    }

    /** 원장: 학생 목록 */
    @GetMapping("/students")
    public List<StudentLiteDto> listStudents() {
        return studentRepo.findAll().stream()
                .map(StudentLiteDto::from)
                .toList();
    }

    /** 원장: 선생 목록 */
    @GetMapping("/teachers")
    public List<TeacherLiteDto> listTeachers() {
        return teacherRepo.findAll().stream()
                .map(TeacherLiteDto::from)
                .toList();
    }

    /** 원장: 학생 삭제 */
    @DeleteMapping("/students/{studentId}")
    public ResponseEntity<Void> deleteStudent(@PathVariable String studentId) {
        studentRepo.deleteByStudentId(studentId);
        return ResponseEntity.noContent().build();
    }

    /** 원장: 선생 삭제 */
    @DeleteMapping("/teachers/{teacherId}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable String teacherId) {
        teacherRepo.deleteByTeacherId(teacherId);
        return ResponseEntity.noContent().build();
    }
}
