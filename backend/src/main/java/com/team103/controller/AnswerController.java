package com.team103.controller;

import com.team103.model.Answer;
import com.team103.model.Parent;
import com.team103.model.Question;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.AnswerRepository;
import com.team103.repository.ParentRepository;
import com.team103.repository.QuestionRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
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

    @Autowired private AnswerRepository answerRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ParentRepository parentRepository;
    @Autowired private FcmService fcmService;

    // 특정 질문의 답변 목록 조회
    @GetMapping("/api/questions/{qId}/answers")
    public List<Answer> listAnswers(@PathVariable("qId") String questionId) {
        List<Answer> list = answerRepository.findActiveByQuestionId(questionId);
        for (Answer a : list) {
            a.setTeacherName(resolveTeacherName(a.getAuthor()));
        }
        return list;
    }

    // 답변 생성
    @PostMapping("/api/questions/{qId}/answers")
    public ResponseEntity<Answer> createAnswer(
            @PathVariable("qId") String questionId,
            @RequestBody Answer payload,
            HttpSession session) {

        Question q = questionRepository.findById(questionId).orElse(null);
        if (q == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        // 저장
        Answer a = new Answer();
        a.setQuestionId(questionId);
        a.setContent(payload.getContent());
        a.setCreatedAt(new Date());

        // 세션에서 작성자
        String role = (String) session.getAttribute("role");        // "teacher" | "student" | "parent"
        String userId = (String) session.getAttribute("username");  // 로그인 시 저장한 키와 통일
        a.setAuthor(userId);

        Answer saved = answerRepository.save(a);

        // ---- FCM 알림 (항상 fcmToken만 사용) ----
        try {
            if ("teacher".equalsIgnoreCase(role)) {
                // 교사가 보냄 → 해당 학생(+학부모)에게
                String studentId = q.getRoomStudentId(); // 질문이 속한 학생 ID
                if (studentId != null && !studentId.isEmpty()) {
                    // 학생
                    Student s = studentRepository.findByStudentId(studentId);
                    if (s != null && s.getFcmToken() != null && !s.getFcmToken().isEmpty()) {
                        fcmService.sendMessageTo(
                                studentId,
                                s.getFcmToken(),
                                "새 답변 알림",
                                "선생님의 답변이 도착했습니다."
                        );
                    }
                    // 학부모(선택) : 해당 학생을 포함하는 학부모 전원
                    List<Parent> parents = parentRepositoryFindByStudentId(studentId);
                    if (parents != null) {
                        for (Parent p : parents) {
                            if (p.getFcmToken() != null && !p.getFcmToken().isEmpty()) {
                                // 부모 userId는 도메인 키(예: parentsId) 사용
                                String parentsId = p.getParentsId();
                                fcmService.sendMessageTo(
                                        parentsId,
                                        p.getFcmToken(),
                                        "새 답변 알림",
                                        "자녀 질문에 답변이 도착했습니다."
                                );
                            }
                        }
                    }
                }
            } else {
                // 학생/학부모가 보냄 → 같은 학원 교사들에게
                int academyNumber = q.getAcademyNumber();
                List<Teacher> teachers = teacherRepositoryFindByAcademyNumber(academyNumber);
                if (teachers != null) {
                    for (Teacher t : teachers) {
                        if (t.getFcmToken() != null && !t.getFcmToken().isEmpty()) {
                            fcmService.sendMessageTo(
                                    t.getTeacherId(),
                                    t.getFcmToken(),
                                    "새 메시지 알림",
                                    "새 메시지가 도착했습니다."
                            );
                        }
                    }
                }
            }
        } catch (Exception ignore) {
            // 알림 실패는 저장과 분리
        }

        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    private List<Parent> parentRepositoryFindByStudentId(String studentId) {

        return parentRepository.findByStudentId(studentId);
    }

    private List<Teacher> teacherRepositoryFindByAcademyNumber(int academyNumber) {

        return teacherRepository.findByAcademyNumber(academyNumber);
    }

    // 답변 수정
    @PutMapping("/api/answers/{id}")
    public ResponseEntity<Answer> updateAnswer(@PathVariable String id,
                                               @RequestBody Answer answer) {
        Optional<Answer> opt = answerRepository.findById(id);
        if (!opt.isPresent()) return ResponseEntity.notFound().build();

        Answer existing = opt.get();
        existing.setContent(answer.getContent());
        Answer updated = answerRepository.save(existing);

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
        if (!opt.isPresent()) return ResponseEntity.notFound().build();

        Answer a = opt.get();
        a.setTeacherName(resolveTeacherName(a.getAuthor()));
        return ResponseEntity.ok(a);
    }

    private String resolveTeacherName(String teacherId) {
        if (teacherId == null) return "";
        Teacher t = teacherRepository.findByTeacherId(teacherId);
        return (t != null && t.getTeacherName() != null && !t.getTeacherName().isEmpty())
                ? t.getTeacherName()
                : teacherId;
    }
}
