package com.team103.controller;

import com.team103.dto.FindIdRequest;
import com.team103.dto.DirectorSignupRequest;
import com.team103.model.Director;
import com.team103.repository.DirectorRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

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

    /** 원장 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody DirectorSignupRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank())
            return ResponseEntity.badRequest().body("username is required");
        if (req.getPassword() == null || req.getPassword().isBlank())
            return ResponseEntity.badRequest().body("password is required");

        if (directorRepo.existsByUsername(req.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("username already exists");
        }

        String encodedPw = passwordEncoder.encode(req.getPassword());

        Director d = new Director();
        d.setName(req.getName());
        d.setUsername(req.getUsername());
        d.setPassword(encodedPw);
        d.setPhone(req.getPhone());
        d.setAcademyNumbers(req.getAcademyNumbers());

        directorRepo.save(d);

        return ResponseEntity.status(HttpStatus.CREATED).body("director signup success");
    }

    /** 원장 단건 조회 (username 기준) */
    @GetMapping("/{username}")
    public ResponseEntity<Director> getByUsername(@PathVariable String username) {
        Director d = directorRepo.findByUsername(username);
        if (d == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(d);
    }

    /** 아이디 찾기 (이름 + 전화번호 → username 반환) */
    @PostMapping("/find_id")
    public ResponseEntity<Map<String,String>> findDirectorId(@RequestBody FindIdRequest req) {
        String phone = req.normalizedPhone();
        Director d = directorRepo.findByNameAndPhone(req.getName(), phone);
        if (d == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        return ResponseEntity.ok(Map.of("username", d.getUsername()));
    }
}
