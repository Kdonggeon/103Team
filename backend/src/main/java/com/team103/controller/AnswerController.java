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
import com.team103.security.JwtUtil;
import com.team103.service.FcmService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.*;

@RestController
public class AnswerController {

    private static final String BEARER = "Bearer ";

    @Autowired private AnswerRepository answerRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ParentRepository parentRepository;
    @Autowired private FcmService fcmService;
    @Autowired private JwtUtil jwtUtil;

    /**
     * Authorization 헤더(Bearer 토큰)가 있으면 세션에 username/role을 채워줌.
     * (필터와 병행 사용 시에도 문제 없음: 세션이 비어있을 때만 세팅)
     */
    @ModelAttribute
    public void ensureSessionFromJwt(
            @RequestHeader(value = "Authorization", required = false) String auth,
            HttpSession session
    ) {
        if (!StringUtils.hasText(auth) || !auth.startsWith(BEARER)) return;

        Object u = session.getAttribute("username");
        Object r = session.getAttribute("role");
        if (u != null && r != null) return;

        try {
            String token = auth.substring(BEARER.length());
            Claims claims = jwtUtil.validateToken(token);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            if (u == null && StringUtils.hasText(userId)) {
                session.setAttribute("username", userId);
            }
            if (r == null && StringUtils.hasText(role)) {
                session.setAttribute("role", role);
            }
        } catch (Exception ignore) {
            // 유효하지 않은 토큰: 세션은 그대로 둠
        }
    }

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

        // 작성자(세션)
        String role = (String) session.getAttribute("role");        // "teacher" | "director" | "student" | "parent"
        String userId = (String) session.getAttribute("username");  // 로그인 시 저장한 키와 통일
        if (!StringUtils.hasText(userId)) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

        // 저장
        Answer a = new Answer();
        a.setQuestionId(questionId);
        a.setContent(payload.getContent() == null ? "" : payload.getContent());
        a.setCreatedAt(new Date());
        a.setAuthor(userId);

        // (선택) authorRole 필드가 있는 엔티티라면 반영
        try { a.getClass().getMethod("setAuthorRole", String.class).invoke(a, role); } catch (Exception ignore) {}

        Answer saved = answerRepository.save(a);

        // ---- FCM 알림 ----
        try {
            Set<String> sentTokens = new HashSet<>();

            if ("teacher".equalsIgnoreCase(role) || "director".equalsIgnoreCase(role)) {
                // 교사/원장 → 학부모(우선), 학생(있다면)

                // 1) parent 전용 방이면 roomParentId 기준
                String parentIdInRoom = q.getRoomParentId();
                if (StringUtils.hasText(parentIdInRoom)) {
                    Parent p = parentRepository.findByParentsId(parentIdInRoom);
                    if (p != null && StringUtils.hasText(p.getFcmToken()) && sentTokens.add(p.getFcmToken())) {
                        fcmService.sendMessageTo(
                                p.getParentsId(),
                                p.getFcmToken(),
                                "새 답변 알림",
                                "선생님의 답변이 도착했습니다."
                        );
                    }
                }

                // 2) student 전용 방이면 학생 + 그 학생의 학부모 전원
                String studentIdInRoom = q.getRoomStudentId();
                if (StringUtils.hasText(studentIdInRoom)) {
                    // 학생
                    Student s = studentRepository.findByStudentId(studentIdInRoom);
                    if (s != null && StringUtils.hasText(s.getFcmToken()) && sentTokens.add(s.getFcmToken())) {
                        fcmService.sendMessageTo(
                                s.getStudentId(),
                                s.getFcmToken(),
                                "새 답변 알림",
                                "선생님의 답변이 도착했습니다."
                        );
                    }
                    // 학부모들 (⚠️ ParentRepository에 findByStudentId 필요)
                    List<Parent> parents = parentRepository.findByStudentId(studentIdInRoom);
                    if (parents != null) {
                        for (Parent p : parents) {
                            if (StringUtils.hasText(p.getFcmToken()) && sentTokens.add(p.getFcmToken())) {
                                fcmService.sendMessageTo(
                                        p.getParentsId(),
                                        p.getFcmToken(),
                                        "새 답변 알림",
                                        "자녀 질문에 답변이 도착했습니다."
                                );
                            }
                        }
                    }
                }

            } else {
                // 학생/학부모 → 같은 학원 교사들
                int academyNumber = q.getAcademyNumber();
                List<Teacher> teachers = teacherRepository.findByAcademyNumber(academyNumber);
                if (teachers != null) {
                    for (Teacher t : teachers) {
                        if (StringUtils.hasText(t.getFcmToken()) && sentTokens.add(t.getFcmToken())) {
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

        // 응답 직전 표시용 이름 세팅(클라이언트 즉시 렌더링 대비)
        saved.setTeacherName(resolveTeacherName(saved.getAuthor()));
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    // 답변 수정
    @PutMapping("/api/answers/{id}")
    public ResponseEntity<Answer> updateAnswer(@PathVariable String id,
                                               @RequestBody Answer answer) {
        Optional<Answer> opt = answerRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

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
        if (opt.isEmpty()) return ResponseEntity.noContent().build();

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
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Answer a = opt.get();
        a.setTeacherName(resolveTeacherName(a.getAuthor()));
        return ResponseEntity.ok(a);
    }

    private String resolveTeacherName(String teacherId) {
        if (!StringUtils.hasText(teacherId)) return "";
        Teacher t = teacherRepository.findByTeacherId(teacherId);
        return (t != null && StringUtils.hasText(t.getTeacherName()))
                ? t.getTeacherName()
                : teacherId;
    }
}
