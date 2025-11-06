package com.team103.controller;

import com.team103.dto.FindIdRequest;
import com.team103.model.Teacher;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;
import com.team103.repository.TeacherRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherRepository teacherRepo;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private CourseRepository courseRepo;

    @Autowired
    private AttendanceRepository attendanceRepo;

    @Autowired
    public TeacherController(TeacherRepository teacherRepo,
                             PasswordEncoder passwordEncoder) {
        this.teacherRepo = teacherRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /** 교사 전체 조회 */
    @GetMapping
    public List<Teacher> getAll() {
        return teacherRepo.findAll();
    }

    /** 교사 생성 (비밀번호 해시 후 저장) */
    @PostMapping
    public Teacher create(@RequestBody Teacher teacher) {
        String raw = teacher.getTeacherPw();
        if (raw != null && !raw.isBlank()) {
            teacher.setTeacherPw(passwordEncoder.encode(raw));
        }
        return teacherRepo.save(teacher);
    }

    /** 특정 교사 단건 조회 (선택) */
    @GetMapping("/{teacherId}")
    public ResponseEntity<Teacher> getOne(@PathVariable String teacherId) {
        Teacher t = teacherRepo.findByTeacherId(teacherId);
        return (t == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(t);
    }

    /** FCM 토큰 업데이트 */
    @PutMapping("/{teacherId}/fcm-token")
    public ResponseEntity<Void> updateFcmToken(@PathVariable String teacherId,
                                               @RequestParam("token") String token) {
        Teacher t = teacherRepo.findByTeacherId(teacherId);
        if (t == null) return ResponseEntity.notFound().build();
        t.setFcmToken((token == null || token.isBlank()) ? null : token);
        teacherRepo.save(t);
        return ResponseEntity.ok().build();
    }

    /** 아이디 찾기 (이름 + 전화번호) */
    @PostMapping("/find_id")
    public ResponseEntity<Map<String,String>> findTeacherId(@RequestBody FindIdRequest req) {
        String phone = req.normalizedPhone();
        var t = teacherRepo.findByTeacherNameAndTeacherPhoneNumber(req.getName(), phone);
        if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        return ResponseEntity.ok(Map.of("username", t.getTeacherId()));
    }
}
