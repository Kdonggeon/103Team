package com.team103.controller;

import com.team103.model.Notice;
import com.team103.repository.NoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Autowired
    private NoticeRepository noticeRepo;

    // 공지 목록 조회
    @GetMapping
    public List<Notice> listAll() {
        return noticeRepo.findAll();
    }

    // 단건 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<Notice> getOne(@PathVariable String id) {
        return noticeRepo.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // 공지 등록
    @PostMapping
    public ResponseEntity<Notice> create(@RequestBody Notice notice) {
        // author 필드는 요청 JSON에 포함
        Notice saved = noticeRepo.save(notice);
        return ResponseEntity.ok(saved);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Notice> updateNotice(
            @PathVariable String id,
            @RequestBody Notice notice
    ) {
        return noticeRepo.findById(id)
            .map(existing -> {
                existing.setTitle(notice.getTitle());
                existing.setContent(notice.getContent());
                // 작성자(author), 생성일(createdAt)은 보존
                Notice updated = noticeRepo.save(existing);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable String id) {
        if (!noticeRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        noticeRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
}
