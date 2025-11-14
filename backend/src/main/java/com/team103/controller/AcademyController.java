package com.team103.controller;

import com.team103.model.Academy;
import com.team103.model.Student;
import com.team103.repository.AcademyRepository;
import com.team103.repository.StudentRepository;
import com.team103.service.AcademyCreateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academy")
public class AcademyController {

    private final AcademyRepository academyRepo;
    private final AcademyCreateService academyCreateService;
    private final StudentRepository studentRepository; // ⭐ 추가

    public AcademyController(AcademyRepository academyRepo,
                             AcademyCreateService academyCreateService,
                             StudentRepository studentRepository) {
        this.academyRepo = academyRepo;
        this.academyCreateService = academyCreateService;
        this.studentRepository = studentRepository; // ⭐ 추가
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

    // ⭐⭐ 학원 → 학생 목록 조회 (핵심)
    @GetMapping("/{academyNumber}/students")
    public ResponseEntity<List<Student>> getStudentsByAcademy(@PathVariable int academyNumber) {

        List<Student> students =
                studentRepository.findByAcademyNumbersContaining(academyNumber);

        return ResponseEntity.ok(students);
    }

    // ====== 새 학원 추가(원장 전용) ======
    public static class CreateAcademyRequest {
        public String name;
        public String phone;
        public String address;
    }

    @PostMapping("/directors/{username}")
    public ResponseEntity<Academy> createForDirector(
            @PathVariable("username") String username,
            @RequestBody CreateAcademyRequest body
    ) {
        Academy saved = academyCreateService.createForDirectorUsername(
                username,
                body != null ? body.name : null,
                body != null ? body.phone : null,
                body != null ? body.address : null
        );
        return ResponseEntity.ok(saved);
    }
}
