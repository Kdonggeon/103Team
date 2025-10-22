package com.team103.controller;

import com.team103.dto.QnaRecentResponse;
import com.team103.model.Answer;
import com.team103.model.Question;
import com.team103.repository.AnswerRepository;
import com.team103.repository.QuestionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/qna")
public class QnaRecentController {

    @Autowired private QuestionRepository questionRepository;
    @Autowired private AnswerRepository answerRepository;

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentQna(HttpServletRequest request) {
        // 세션에서 username/role 확보 (프로젝트 공통 세션 주입 로직과 동일 키 사용)
        String userId = (String) request.getSession().getAttribute("username");
        String role   = (String) request.getSession().getAttribute("role");
        if (userId == null || role == null) {
            return ResponseEntity.status(401).body("UNAUTHORIZED");
        }
        role = role.toLowerCase(Locale.ROOT);

        if (role.equals("student") || role.equals("parent")) {
            // 1순위: 내가 쓴 질문들(questionIds)에 대한 최신 답변
            List<Question> myQuestions = questionRepository
                    .findByAuthorAndAuthorRoleOrderByCreatedAtDesc(userId, role);
            if (myQuestions != null && !myQuestions.isEmpty()) {
                List<String> qids = myQuestions.stream().map(Question::getId).collect(Collectors.toList());
                Answer latestAnswer = answerRepository
                        .findTopByQuestionIdInAndDeletedFalseOrderByCreatedAtDesc(qids);
                if (latestAnswer != null) {
                    return ResponseEntity.ok(
                            new QnaRecentResponse(latestAnswer.getQuestionId(), latestAnswer.getId(), "ANSWER")
                    );
                }
                // 2순위: 내가 쓴 최신 질문 1건
                Question latestQ = myQuestions.get(0); // 이미 최신순
                return ResponseEntity.ok(new QnaRecentResponse(latestQ.getId(), null, "QUESTION"));
            }
            return ResponseEntity.noContent().build();
        }

        if (role.equals("teacher") || role.equals("director")) {
            // 1순위: 내가 쓴 최신 답변 1건
            Answer latestMyAnswer = answerRepository.findTopByAuthorOrderByCreatedAtDesc(userId);
            if (latestMyAnswer != null) {
                return ResponseEntity.ok(
                        new QnaRecentResponse(latestMyAnswer.getQuestionId(), latestMyAnswer.getId(), "ANSWER")
                );
            }
            // 2순위: 전체 최신 질문 1건
            Question latestQ = questionRepository.findTopByOrderByCreatedAtDesc();
            if (latestQ != null) {
                return ResponseEntity.ok(new QnaRecentResponse(latestQ.getId(), null, "QUESTION"));
            }
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.status(403).body("FORBIDDEN");
    }
}
