package com.team103.controller;

import com.team103.model.Question;
import com.team103.repository.QuestionRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
    @Autowired
    private QuestionRepository questionRepository;
    
    
    // 전체 질문 조회
    @GetMapping
    public List<Question> listQuestions() {
        return questionRepository.findAll();
    }

    // 단일 질문 조회
    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestion(@PathVariable String id) {
        Optional<Question> opt = questionRepository.findById(id);
        return opt.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    // 질문 생성
    @PostMapping
    public ResponseEntity<?> createQuestion(
            @RequestBody Question question,
            HttpSession session) {

        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");

        if (userId == null || role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 후 이용해주세요.");
        }

        question.setAuthor(userId);
        question.setAuthorRole(role);

        Question saved = questionRepository.save(question);
        return ResponseEntity.ok(saved);
    }
    // 질문 수정
    @PutMapping("/{id}")
    public ResponseEntity<Question> updateQuestion(
            @PathVariable String id,
            @RequestBody Question question) {
        if (!questionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        question.setId(id);
        Question updated = questionRepository.save(question);
        return ResponseEntity.ok(updated);
    }

    // 질문 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable String id) {
        if (!questionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        questionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping(params = "academyNumber")
    public List<Question> findByAcademyNumber(@RequestParam("academyNumber") int academyNumber) {
        return questionRepository.findByAcademyNumber(academyNumber);
    }
    
}
