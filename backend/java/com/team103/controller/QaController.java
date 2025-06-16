package com.team103.controller;

import com.team103.model.Qa;
import com.team103.repository.QaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/qna")
public class QaController {

    @Autowired
    private QaRepository qaRepo;

    // QA 전체 목록 조회
    @GetMapping
    public List<Qa> listAll() {
        return qaRepo.findAll();
    }

    // 단건 QA 조회
    @GetMapping("/{id}")
    public ResponseEntity<Qa> getOne(@PathVariable String id) {
        return qaRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // QA 생성
    @PostMapping
    public ResponseEntity<Qa> create(@RequestBody Qa qa) {
        Qa saved = qaRepo.save(qa);
        return ResponseEntity.ok(saved);
    }

    // QA 수정
    @PutMapping("/{id}")
    public ResponseEntity<Qa> updateQa(
            @PathVariable String id,
            @RequestBody Qa qa) {
        return qaRepo.findById(id)
                .map(existing -> {
                    existing.setTitle(qa.getTitle());
                    existing.setContent(qa.getContent());
                    Qa updated = qaRepo.save(existing);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // QA 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQa(@PathVariable String id) {
        if (!qaRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        qaRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}