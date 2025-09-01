package com.team103.controller;

import com.team103.model.Question;
import com.team103.repository.QuestionRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
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

        //  학생/학부모만 방 생성 허용
        if (!(role.equalsIgnoreCase("student") || role.equalsIgnoreCase("parent"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("학생 또는 학부모만 질문을 생성할 수 있습니다.");
        }

        question.setAuthor(userId);
        question.setAuthorRole(role);
        question.setCreatedAt(new Date()); // 생성 시각 보장

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
