package com.team103.controller;

import com.team103.dto.DirectorSignupRequest;
import com.team103.model.Director;
import com.team103.repository.DirectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/signup/director")
public class DirectorSignupController {

    @Autowired private DirectorRepository directorRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> signup(@RequestBody DirectorSignupRequest req) {
        // 1) 필수값 검증
        if (isBlank(req.getUsername()) || isBlank(req.getPassword()) || isBlank(req.getName())) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "fail",
                "message", "필수 항목(username, password, name)이 비어있습니다."
            ));
        }

        // 2) 중복 체크 (로그인 아이디 기준)
        if (directorRepo.existsByUsername(req.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", "fail",
                "message", "이미 존재하는 원장 아이디입니다."
            ));
        }

        // 3) 비밀번호 암호화
        final String encPw = passwordEncoder.encode(req.getPassword());

        // 4) 학원 번호 6자리 자동 생성 (100000 ~ 999999)
        int academyNumber = generateAcademyNumber();
        List<Integer> academies = List.of(academyNumber);

        // 5) 엔티티 생성 및 저장
        Director director = new Director();
        director.setUsername(req.getUsername());
        director.setPassword(encPw);
        director.setName(req.getName());
        director.setPhone(req.getPhone());
        director.setAcademyNumbers(academies);

        directorRepo.save(director);

        // 6) 성공 응답 (발급된 학원번호 같이 리턴)
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "원장 회원가입 성공",
            "username", director.getUsername(),
            "academyNumber", academyNumber,
            "academyNumbers", academies
        ));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 다른 원장이 쓰고 있지 않은 6자리 학원 번호 생성 */
    private int generateAcademyNumber() {
        int n;
        do {
            n = 100000 + (int) (Math.random() * 900000); // 100000 ~ 999999
        } while (directorRepo.existsByAcademyNumbersContains(n));
        return n;
    }
} 


