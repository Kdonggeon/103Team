package com.team103.controller;

import com.team103.model.Teacher;
import com.team103.repository.TeacherRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherRepository teacherRepo;

    public TeacherController(TeacherRepository teacherRepo) {
        this.teacherRepo = teacherRepo;
    }

    @GetMapping
    public List<Teacher> getAll() {
        return teacherRepo.findAll();
    }

    @PostMapping
    public Teacher create(@RequestBody Teacher teacher) {
        return teacherRepo.save(teacher);
    }
}
