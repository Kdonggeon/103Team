package com.team103.controller;

import com.team103.model.QaAnswer;
import com.team103.repository.QaAnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/qa")  // 프론트 요청과 일치하도록 수정
public class QaAnswerController {

    @Autowired
    private QaAnswerRepository qaAnswerRepo;

    // 답변 전체 조회 (디버깅용)
    @GetMapping("/answers")
    public List<QaAnswer> getAll() {
        return qaAnswerRepo.findAll();
    }

    // 특정 QA에 대한 답변 조회
    @GetMapping("/{qaId}/answer")
    public ResponseEntity<List<QaAnswer>> getByQaId(@PathVariable String qaId) {
        List<QaAnswer> answers = qaAnswerRepo.findAllByQaId(qaId);
        if (answers.isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(answers);
        }
    }
    

    // 특정 QA에 대한 답변 등록
    @PostMapping("/qa/{qaId}/answer")
    public ResponseEntity<QaAnswer> createAnswerWithQaId(@PathVariable String qaId, @RequestBody QaAnswer answer) {
        answer.setQaId(qaId);
        answer.setCreatedAt(new Date());
        QaAnswer saved = qaAnswerRepo.save(answer);
        return ResponseEntity.ok(saved);
    }

    // 답변 수정
    @PutMapping("/answers/{id}")
    public ResponseEntity<QaAnswer> update(@PathVariable String id, @RequestBody QaAnswer updatedAnswer) {
        return qaAnswerRepo.findById(id)
                .map(existing -> {
                    existing.setContent(updatedAnswer.getContent());
                    existing.setAuthorId(updatedAnswer.getAuthorId());
                    existing.setAuthorRole(updatedAnswer.getAuthorRole());
                    existing.setUpdatedAt(new Date());
                    QaAnswer saved = qaAnswerRepo.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // 답변 삭제
    @DeleteMapping("/answers/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!qaAnswerRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        qaAnswerRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
