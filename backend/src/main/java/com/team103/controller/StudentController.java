package com.team103.controller;

import com.team103.model.Student;
import com.team103.repository.StudentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository repo;

    public StudentController(StudentRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Student> getAllStudents() {
        return repo.findAll();
    }

    @PostMapping
    public Student createStudent(@RequestBody Student student) {
        return repo.save(student);
    }
    
    @GetMapping("/{id}")
    public Student getStudentByStudentId(@PathVariable("id") long studentId) {
        return repo.findByStudentId(studentId);
    }

}
