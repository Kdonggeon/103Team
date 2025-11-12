package com.team103.controller;

import com.team103.model.Academy;
import com.team103.model.Student;
import com.team103.repository.AcademyRepository;
<<<<<<< HEAD
import com.team103.service.AcademyCreateService;
import org.springframework.http.ResponseEntity;
=======
import com.team103.repository.StudentRepository;
>>>>>>> new2
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academy")
public class AcademyController {

    private final AcademyRepository academyRepo;
<<<<<<< HEAD
    private final AcademyCreateService academyCreateService;

    public AcademyController(AcademyRepository academyRepo,
                             AcademyCreateService academyCreateService) {
        this.academyRepo = academyRepo;
        this.academyCreateService = academyCreateService;
=======
    private final StudentRepository studentRepo;

    public AcademyController(AcademyRepository academyRepo, StudentRepository studentRepo) {
        this.academyRepo = academyRepo;
        this.studentRepo = studentRepo;
>>>>>>> new2
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

<<<<<<< HEAD
    // ====== 새 학원 추가(원장 전용): 랜덤 4자리 학원번호(중복 방지) 생성 후 저장 ======
    public static class CreateAcademyRequest {
        public String name;    // Academy_Name
        public String phone;   // Academy_Phone_Number
        public String address; // Academy_Address
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
=======
    // ✅ 특정 학원 번호의 학생 목록 조회 (이름 검색 제거 버전)
    @GetMapping("/{academyNumber}/students")
    public List<Student> getStudentsByAcademy(@PathVariable Integer academyNumber) {
        return studentRepo.findByAcademyNumber(academyNumber);
>>>>>>> new2
    }
}
