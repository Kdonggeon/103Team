package com.team103.controller;

import com.team103.model.Academy;
import com.team103.repository.AcademyRepository;
import com.team103.service.AcademyCreateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academy")
public class AcademyController {

    private final AcademyRepository academyRepo;
    private final AcademyCreateService academyCreateService;

    public AcademyController(AcademyRepository academyRepo,
                             AcademyCreateService academyCreateService) {
        this.academyRepo = academyRepo;
        this.academyCreateService = academyCreateService;
    }

    @GetMapping
    public List<Academy> getAll() {
        return academyRepo.findAll();
    }

    @PostMapping
    public Academy create(@RequestBody Academy academy) {
        return academyRepo.save(academy);
    }

    @GetMapping("/{name}")
    public Academy getByName(@PathVariable String name) {
        return academyRepo.findByName(name);
    }

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
    }
}
