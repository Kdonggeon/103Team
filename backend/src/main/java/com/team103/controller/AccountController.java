package com.team103.controller;

import com.team103.dto.AccountDeleteRequest;
import com.team103.model.*;
import com.team103.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "*")
public class AccountController {

    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;
    private final ParentRepository parentRepo;
    private final DirectorRepository directorRepo;
    private final PasswordEncoder passwordEncoder;

    public AccountController(StudentRepository studentRepo,
                             TeacherRepository teacherRepo,
                             ParentRepository parentRepo,
                             DirectorRepository directorRepo,
                             PasswordEncoder passwordEncoder) {
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
        this.parentRepo = parentRepo;
        this.directorRepo = directorRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@Valid @RequestBody AccountDeleteRequest req) {

        String role = req.getRole().trim().toLowerCase();
        String id   = req.getId().trim();

        switch (role) {
            case "student": {
                Student s = studentRepo.findByStudentId(id);
                if (s == null) return ResponseEntity.notFound().build();
                if (!passwordEncoder.matches(req.getPassword(), s.getStudentPw()))
                    return ResponseEntity.status(403).body(Map.of("message", "비밀번호가 일치하지 않습니다."));
                studentRepo.delete(s);
                break;
            }
            case "teacher": {
                Teacher t = teacherRepo.findByTeacherId(id);
                if (t == null) return ResponseEntity.notFound().build();
                if (!passwordEncoder.matches(req.getPassword(), t.getTeacherPw()))
                    return ResponseEntity.status(403).body(Map.of("message", "비밀번호가 일치하지 않습니다."));
                teacherRepo.delete(t);
                break;
            }
            case "parent": {
                Parent p = parentRepo.findByParentsId(id);
                if (p == null) return ResponseEntity.notFound().build();
                if (!passwordEncoder.matches(req.getPassword(), p.getParentsPw()))
                    return ResponseEntity.status(403).body(Map.of("message", "비밀번호가 일치하지 않습니다."));
                parentRepo.delete(p);
                break;
            }
            case "director": {
                Director d = directorRepo.findByUsername(id);
                if (d == null) return ResponseEntity.notFound().build();
                if (!passwordEncoder.matches(req.getPassword(), d.getPassword()))
                    return ResponseEntity.status(403).body(Map.of("message", "비밀번호가 일치하지 않습니다."));
                directorRepo.delete(d);
                break;
            }
            default:
                return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 역할입니다."));
        }

        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
