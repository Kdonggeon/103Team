package com.team103.controller;

import com.team103.model.Academy;
import com.team103.model.Student;
import com.team103.repository.AcademyRepository;
import com.team103.repository.StudentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academy")
public class AcademyController {

    private final AcademyRepository academyRepo;
    private final StudentRepository studentRepo;

    public AcademyController(AcademyRepository academyRepo, StudentRepository studentRepo) {
        this.academyRepo = academyRepo;
        this.studentRepo = studentRepo;
    }

    // ✅ 모든 학원 목록
    @GetMapping
    public List<Academy> getAll() {
        return academyRepo.findAll();
    }

    // ✅ 학원 생성
    @PostMapping
    public Academy create(@RequestBody Academy academy) {
        return academyRepo.save(academy);
    }

    // ✅ 학원 이름으로 조회
    @GetMapping("/{name}")
    public Academy getByName(@PathVariable String name) {
        return academyRepo.findByName(name);
    }

    // ✅ 특정 학원 번호의 학생 목록 조회 (이름 검색 제거 버전)
    @GetMapping("/{academyNumber}/students")
    public List<Student> getStudentsByAcademy(@PathVariable Integer academyNumber) {
        return studentRepo.findByAcademyNumber(academyNumber);
    }
}
