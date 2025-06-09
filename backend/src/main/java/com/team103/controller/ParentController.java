package com.team103.controller;

import com.team103.dto.ParentSignupRequest;
import com.team103.model.Parent;
import com.team103.repository.ParentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parents")
@CrossOrigin(origins = "*")
public class ParentController {

    private final ParentRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ParentController(ParentRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Parent> getAllParents() {
        return repo.findAll();
    }

    @PostMapping
    public ResponseEntity<?> registerParent(@RequestBody ParentSignupRequest request) {
        if (request.getParentsPw() == null || request.getParentsPw().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("비밀번호는 필수 입력 항목입니다.");
        }

        String encodedPw = passwordEncoder.encode(request.getParentsPw());
        Parent parent = request.toEntity(encodedPw);  

        Parent saved = repo.save(parent);
        return ResponseEntity.ok(saved);
    }


    @GetMapping("/{id}")
    public Parent getById(@PathVariable String id) {
        return repo.findByParentsId(id);
    }
}
