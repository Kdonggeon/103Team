package com.team103.controller;

import com.team103.model.Answer;
import com.team103.repository.AnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
public class AnswerController {
    @Autowired
    private AnswerRepository answerRepository;

    // 특정 질문의 답변 목록 조회
    @GetMapping("/api/questions/{qId}/answers")
    public List<Answer> listAnswers(@PathVariable("qId") String questionId) {
        return answerRepository.findByQuestionId(questionId);
    }

    // 답변 생성
    @PostMapping("/api/questions/{qId}/answers")
    public Answer createAnswer(@PathVariable("qId") String questionId,
                               @RequestBody Answer answer) {

        // ✅ 로그인 사용자 ID (실제는 세션이나 토큰에서 꺼냄)
        String loggedInUserId = "teacher001";  // 예시로 하드코딩

        answer.setQuestionId(questionId);
        answer.setAuthor(loggedInUserId);     // 서버가 작성자 세팅
        answer.setCreatedAt(new Date());      // 생성 일시 세팅
        return answerRepository.save(answer);
    }

    // 답변 수정
    @PutMapping("/api/answers/{id}")
    public ResponseEntity<Answer> updateAnswer(
            @PathVariable String id,
            @RequestBody Answer answer) {
        Optional<Answer> opt = answerRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        answer.setId(id);
        answer.setQuestionId(opt.get().getQuestionId());
        Answer updated = answerRepository.save(answer);
        return ResponseEntity.ok(updated);
    }

    // 답변 삭제
    @DeleteMapping("/api/answers/{id}")
    public ResponseEntity<Void> deleteAnswer(@PathVariable String id) {
        if (!answerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        answerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
