package com.team103.controller;

import com.team103.model.Answer;
import com.team103.model.Question;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.model.Parent;
import com.team103.repository.AnswerRepository;
import com.team103.repository.QuestionRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import com.team103.repository.ParentRepository;
import com.team103.service.FcmService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        List<Answer> list = answerRepository.findActiveByQuestionId(questionId);
        // ✅ 각 답변에 교사이름 세팅
        for (Answer a : list) {
            a.setTeacherName(resolveTeacherName(a.getAuthor()));
        }
        return list;
    }

    // 답변 생성
    @PostMapping("/api/questions/{qId}/answers")
    public ResponseEntity<?> createAnswer(@PathVariable("qId") String questionId,
                                          @RequestBody Answer answer,
                                          HttpSession session) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");

        if (userId == null || role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 후 이용해주세요.");
        }
        if (!"teacher".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("선생만 답변을 작성할 수 있습니다.");
        }

        // 1) 질문 존재 확인
        Optional<Question> optQ = questionRepository.findById(questionId);
        if (!optQ.isPresent()) {
            return ResponseEntity.badRequest().body("질문이 존재하지 않습니다.");
        }
        Question question = optQ.get();

        // 2) 필수 필드 세팅
        answer.setQuestionId(questionId);
        answer.setAuthor(userId);
        answer.setDeleted(false);
        if (answer.getCreatedAt() == null) {
            answer.setCreatedAt(new Date());
        }

        // 3) 저장
        Answer savedAnswer = answerRepository.save(answer);

        // 4) 응답에 교사이름 세팅
        savedAnswer.setTeacherName(resolveTeacherName(savedAnswer.getAuthor()));

        // 5) FCM (실패해도 저장에는 영향 없음)
        try {
            String authorId = question.getAuthor();
            String authorRole = question.getAuthorRole();

            String targetFcmToken = null;
            if (authorId != null && authorRole != null) {
                if ("student".equalsIgnoreCase(authorRole)) {
                    Student student = studentRepository.findByStudentId(authorId);
                    if (student != null) targetFcmToken = student.getFcmToken();
                } else if ("teacher".equalsIgnoreCase(authorRole)) {
                    Teacher teacher = teacherRepository.findByTeacherId(authorId);
                    if (teacher != null) targetFcmToken = teacher.getFcmToken();
                } else if ("parent".equalsIgnoreCase(authorRole)) {
                    Parent parent = parentRepository.findByParentsId(authorId);
                    if (parent != null) targetFcmToken = parent.getFcmToken();
                }
            }

            if (targetFcmToken != null && !targetFcmToken.isEmpty()) {
                fcmService.sendMessageTo(
                        targetFcmToken,
                        "새 답변 알림",
                        "회원님의 질문에 답변이 등록되었습니다."
                );
            }
        } catch (Exception e) {
            System.out.println("[AnswerController] [ERROR] FCM 전송 실패 → " + e.getMessage());
        }

        return ResponseEntity.ok(savedAnswer);
    }

    // 답변 수정
    @PutMapping("/api/answers/{id}")
    public ResponseEntity<Answer> updateAnswer(@PathVariable String id,
                                               @RequestBody Answer answer) {
        Optional<Answer> opt = answerRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Answer existing = opt.get();
        existing.setContent(answer.getContent());
        Answer updated = answerRepository.save(existing);

        // 응답에 교사이름 세팅(선택)
        updated.setTeacherName(resolveTeacherName(updated.getAuthor()));
        return ResponseEntity.ok(updated);
    }

    // 답변 삭제(소프트)
    @DeleteMapping("/api/answers/{id}")
    public ResponseEntity<Void> deleteAnswer(@PathVariable String id,
                                             HttpSession session) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");
        if (userId == null || role == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!"teacher".equalsIgnoreCase(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Optional<Answer> opt = answerRepository.findById(id);
        if (!opt.isPresent()) return ResponseEntity.noContent().build();

        Answer answer = opt.get();
        if (!answer.isDeleted()) {
            answer.setDeleted(true);
            answerRepository.save(answer);
        }
        return ResponseEntity.noContent().build();
    }

    // 단건 조회
    @GetMapping("/api/answers/{id}")
    public ResponseEntity<Answer> getAnswer(@PathVariable String id) {
        Optional<Answer> opt = answerRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Answer a = opt.get();
        a.setTeacherName(resolveTeacherName(a.getAuthor())); // 응답에 교사이름 세팅(선택)
        return ResponseEntity.ok(a);
    }

    private String resolveTeacherName(String teacherId) {
        if (teacherId == null) return "";
        Teacher t = teacherRepository.findByTeacherId(teacherId);
        return (t != null && t.getTeacherName() != null && !t.getTeacherName().isEmpty())
                ? t.getTeacherName()
                : teacherId; // 못 찾으면 ID fallback
    }
}
