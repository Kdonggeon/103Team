package com.team103.controller;

import com.team103.model.Answer;
import com.team103.repository.AnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.team103.model.Question;  
import com.team103.repository.QuestionRepository;
import com.team103.service.FcmService;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.model.Parent;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import com.team103.repository.ParentRepository;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
public class AnswerController {
    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private FcmService fcmService;

    // 특정 질문의 답변 목록 조회
    @GetMapping("/api/questions/{qId}/answers")
    public List<Answer> listAnswers(@PathVariable("qId") String questionId) {
        return answerRepository.findByQuestionIdAndDeletedFalse(questionId);
    }
    // 답변 생성
    @PostMapping("/api/questions/{qId}/answers")
    public ResponseEntity<?> createAnswer(@PathVariable("qId") String questionId,
                                          @RequestBody Answer answer) {
    	System.out.println("[AnswerController] /answers POST 호출됨 → questionId=" + questionId);
        // 1️⃣ 답변 저장
        answer.setQuestionId(questionId);
        answer.setCreatedAt(new Date());
        Answer savedAnswer = answerRepository.save(answer);
        System.out.println("[AnswerController] [DEBUG] Answer 저장 완료 → " + savedAnswer);
        // 2️⃣ 질문 작성자 정보 조회
        Optional<Question> optionalQuestion = questionRepository.findById(questionId);
        System.out.println("[AnswerController] [DEBUG] questionRepository.findById() 결과 → " + optionalQuestion);
        if (!optionalQuestion.isPresent()) {
            System.out.println("[AnswerController] [WARN] 질문 문서를 찾을 수 없음 → questionId=" + questionId);
            return ResponseEntity.badRequest().body("질문이 존재하지 않습니다.");
        }
        Question question = optionalQuestion.get();
        System.out.println("[AnswerController] [DEBUG] Question 객체 획득 완료 → " + question);
        String authorId = question.getAuthor();
        String authorRole = question.getAuthorRole();

        System.out.println("[AnswerController] [DEBUG] authorId=" + authorId + ", authorRole=" + authorRole);

        if (authorId == null || authorRole == null) {
            System.out.println("[AnswerController] [WARN] authorId 또는 authorRole이 null → FCM 발송 건너뜀");
            return ResponseEntity.ok(savedAnswer);
        }

        // 3️⃣ 역할별 FCM 토큰 조회
        String targetFcmToken = null;
        if ("student".equalsIgnoreCase(authorRole)) {
        	    Student student = studentRepository.findByStudentId(authorId);
        	    if (student != null) {
        	        targetFcmToken = student.getFcmToken();
        	   };

        } else if ("teacher".equalsIgnoreCase(authorRole)) {
        	    Teacher teacher = teacherRepository.findByTeacherId(authorId);
        	    if (teacher != null) {
        	        targetFcmToken = teacher.getFcmToken();
        	    }

        } else if ("parent".equalsIgnoreCase(authorRole)) {
        	    Parent parent = parentRepository.findByParentsId(authorId);
        	    if (parent != null) {
        	        targetFcmToken = parent.getFcmToken();
        	    }
        }

        if (targetFcmToken != null && !targetFcmToken.isEmpty()) {
            System.out.println("[AnswerController] FCM 발송 시도 → targetFcmToken=" + targetFcmToken);

            try {
                fcmService.sendMessageTo(
                    targetFcmToken,
                    "새 답변 알림",
                    "회원님의 질문에 답변이 등록되었습니다."
                );
                System.out.println("[AnswerController] [INFO] FCM 알림 전송 완료");
            } catch (Exception e) {
                System.out.println("[AnswerController] [ERROR] FCM 알림 전송 실패 → " + e.getMessage());
                e.printStackTrace();
            }

        } else {
            System.out.println("[AnswerController] FCM 발송 스킵 (토큰이 없음)");
        }
        

        return ResponseEntity.ok(savedAnswer);
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
