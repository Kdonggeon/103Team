package com.team103.controller;

import com.team103.model.Notice;
import com.team103.model.Teacher;
import com.team103.repository.NoticeRepository;
import com.team103.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Autowired
    private NoticeRepository noticeRepo;

    @Autowired
    private TeacherRepository teacherRepo;

    // 공지사항 목록 조회 (author -> teacherName 변환)
    @GetMapping
    public List<Notice> listAll() {
        List<Notice> notices = noticeRepo.findAll();
        for (Notice n : notices) {
            String teacherId = n.getAuthor();
            if (teacherId != null) {
                teacherRepo.findById(teacherId)
                    .ifPresent(t -> n.setAuthor(t.getTeacherName()));
            }
        }
        return notices;
    }

    // 공지사항 단건 조회 (author -> teacherName 변환)
    @GetMapping("/{id}")
    public ResponseEntity<Notice> getOne(@PathVariable String id) {
        Optional<Notice> opt = noticeRepo.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Notice notice = opt.get();
        String teacherId = notice.getAuthor();
        if (teacherId != null) {
            teacherRepo.findById(teacherId)
                .ifPresent(t -> notice.setAuthor(t.getTeacherName()));
        }
        return ResponseEntity.ok(notice);
    }

    // 공지사항 등록 (author 덮어쓰기 제거: 클라이언트에서 보낸 author 그대로 저장)
    @PostMapping
    public ResponseEntity<Notice> create(@RequestBody Notice notice) {
        Notice saved = noticeRepo.save(notice);
        return ResponseEntity.ok(saved);
    }

    // 공지사항 수정
    @PutMapping("/{id}")
    public ResponseEntity<Notice> updateNotice(
            @PathVariable String id,
            @RequestBody Notice notice
    ) {
        return noticeRepo.findById(id)
            .map(existing -> {
                existing.setTitle(notice.getTitle());
                existing.setContent(notice.getContent());
                // author, createdAt 등 기존 필드 유지
                Notice updated = noticeRepo.save(existing);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // 공지사항 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable String id) {
        if (!noticeRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        noticeRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}