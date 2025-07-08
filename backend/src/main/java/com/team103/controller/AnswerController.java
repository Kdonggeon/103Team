package com.team103.controller;

import com.team103.model.Answer;
import com.team103.repository.AnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.team103.model.Question;  
import com.team103.repository.QuestionRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
public class AnswerController {
    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    // 특정 질문의 답변 목록 조회
    @GetMapping("/api/questions/{qId}/answers")
    public List<Answer> listAnswers(@PathVariable("qId") String questionId) {
        return answerRepository.findByQuestionIdAndDeletedFalse(questionId);
    }
    // 답변 생성
    @PostMapping("/api/questions/{qId}/answers")
    public Answer createAnswer(@PathVariable("qId") String questionId,
                               @RequestBody Answer answer) {


        answer.setQuestionId(questionId);     // 서버가 작성자 세팅
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

        Answer existing = opt.get();

        existing.setContent(answer.getContent());

        Answer updated = answerRepository.save(existing);
        return ResponseEntity.ok(updated);
    }

    // 답변 삭제
    @DeleteMapping("/api/answers/{id}")
    public ResponseEntity<Void> deleteAnswer(@PathVariable String id) {
        Optional<Answer> opt = answerRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.noContent().build();
        }

        Answer answer = opt.get();
        if (!answer.isDeleted()) {
            answer.setDeleted(true);
            answerRepository.save(answer);
        }

        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/api/answers/{id}")
    public ResponseEntity<Answer> getAnswer(@PathVariable String id) {
        Optional<Answer> opt = answerRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(opt.get());
    }
    
}
