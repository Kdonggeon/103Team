package com.team103.controller;

import com.team103.dto.PasswordResetRequest;
import com.team103.model.Parent;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.model.Director;                // ← 원장 사용 시
import com.team103.repository.ParentRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import com.team103.repository.DirectorRepository; // ← 원장 사용 시
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reset-password")
@CrossOrigin(origins = "*")
public class PasswordResetController {

    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;
    private final ParentRepository parentRepo;
    private final DirectorRepository directorRepo; // ← 원장 사용 시
    private final PasswordEncoder passwordEncoder;

    public PasswordResetController(StudentRepository studentRepo,
                                   TeacherRepository teacherRepo,
                                   ParentRepository parentRepo,
                                   DirectorRepository directorRepo, // ← 원장 사용 시
                                   PasswordEncoder passwordEncoder) {
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
        this.parentRepo = parentRepo;
        this.directorRepo = directorRepo; // ← 원장 사용 시
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest req) {
        String role = req.getRole() == null ? "" : req.getRole().trim().toLowerCase();
        String id = safe(req.getId());
        String name = safe(req.getName());
        String phone = normalize(req.getPhone());
        String newPw = req.getNewPassword();

        if (newPw == null || newPw.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "새 비밀번호는 필수입니다."));
        }
        if (role.isEmpty() || id.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "role과 id는 필수입니다."));
        }

        switch (role) {
            case "student": {
                Student s = studentRepo.findByStudentId(id);
                if (s == null || !equalsTrim(s.getStudentName(), name) || !phoneEquals(s.getStudentPhoneNumber(), phone)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "정보가 일치하지 않습니다."));
                }
                s.setStudentPw(passwordEncoder.encode(newPw));
                studentRepo.save(s);
                break;
            }
            case "teacher": {
                Teacher t = teacherRepo.findByTeacherId(id);
                if (t == null || !equalsTrim(t.getTeacherName(), name) || !phoneEquals(t.getTeacherPhoneNumber(), phone)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "정보가 일치하지 않습니다."));
                }
                t.setTeacherPw(passwordEncoder.encode(newPw));
                teacherRepo.save(t);
                break;
            }
            case "parent": {
                Parent p = parentRepo.findByParentsId(id);
                if (p == null || !equalsTrim(p.getParentsName(), name) || !phoneEquals(p.getParentsPhoneNumber(), phone)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "정보가 일치하지 않습니다."));
                }
                p.setParentsPw(passwordEncoder.encode(newPw));
                parentRepo.save(p);
                break;
            }
            case "director": { // 원장 지원
                Director d = directorRepo.findByUsername(id);
                if (d == null || !equalsTrim(d.getName(), name) || !phoneEquals(d.getPhone(), phone)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "정보가 일치하지 않습니다."));
                }
                d.setPassword(passwordEncoder.encode(newPw));
                directorRepo.save(d);
                break;
            }
            default:
                return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 역할입니다."));
        }

        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ---------- helpers ----------
    private static String safe(String s) { return s == null ? "" : s.trim(); }

    /** a와 b(이미 normalize된 값)를 숫자만 남기고 비교 */
    private static boolean phoneEquals(String a, String normalizedB) {
        String normA = a == null ? "" : a.replaceAll("\\D", "");
        String normB = normalizedB == null ? "" : normalizedB;
        return normA.equals(normB);
    }

    private static boolean equalsTrim(String a, String b) {
        String aa = a == null ? "" : a.trim();
        String bb = b == null ? "" : b.trim();
        return aa.equals(bb);
    }

    private static String normalize(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }
}
