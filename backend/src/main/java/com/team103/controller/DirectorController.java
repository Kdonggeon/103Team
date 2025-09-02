package com.team103.controller;

import com.team103.dto.DirectorSignupRequest;
import com.team103.model.Director;
import com.team103.repository.DirectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/directors")
@CrossOrigin(origins = "*")
public class DirectorController {

    private final DirectorRepository directorRepo;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DirectorController(DirectorRepository directorRepo, PasswordEncoder passwordEncoder) {
        this.directorRepo = directorRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody DirectorSignupRequest req) {
        // 1) 유효성 체크
        if (req.getUsername() == null || req.getUsername().isBlank())
            return ResponseEntity.badRequest().body("username is required");
        if (req.getPassword() == null || req.getPassword().isBlank())
            return ResponseEntity.badRequest().body("password is required");

        // 2) 중복 체크
        if (directorRepo.existsByUsername(req.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("username already exists");
        }

        // 3) 비밀번호 암호화
        String encodedPw = passwordEncoder.encode(req.getPassword());

        // 4) Entity 생성 & 저장
        Director d = new Director();
        d.setName(req.getName());
        d.setUsername(req.getUsername());
        d.setPassword(encodedPw);
        d.setPhone(req.getPhone());
        d.setAcademyNumbers(req.getAcademyNumbers());

        directorRepo.save(d);

        return ResponseEntity.status(HttpStatus.CREATED).body("director signup success");
    }
}
