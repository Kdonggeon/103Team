package com.team103.controller;

import com.team103.model.Academy;
import com.team103.repository.AcademyRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academy")
public class AcademyController {

    private final AcademyRepository academyRepo;

    public AcademyController(AcademyRepository academyRepo) {
        this.academyRepo = academyRepo;
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
}
