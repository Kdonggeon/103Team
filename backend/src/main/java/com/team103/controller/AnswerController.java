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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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

    /** JWT → 세션에 username/role 저장 */
    @ModelAttribute
    public void ensureSessionFromJwt(
            @RequestHeader(value = "Authorization", required = false) String auth,
            HttpSession session
    ) {
        if (!StringUtils.hasText(auth) || !auth.startsWith(BEARER)) return;

        if (session.getAttribute("username") != null &&
            session.getAttribute("role") != null) return;

        try {
            String token = auth.substring(BEARER.length());
            Claims claims = jwtUtil.validateToken(token);

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            if (StringUtils.hasText(userId)) {
                session.setAttribute("username", userId);
            }
            if (StringUtils.hasText(role)) {
                session.setAttribute("role", role);
            }
        } catch (Exception ignore) {}
    }

    // ─────────────────────────────────────────────────────────────
    // 🔹 특정 질문의 답변 목록 조회
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/api/questions/{qId}/answers")
    public List<Answer> listAnswers(@PathVariable("qId") String questionId) {
        List<Answer> list = answerRepository.findActiveByQuestionId(questionId);
        for (Answer a : list) {
            a.setTeacherName(resolveTeacherName(a.getAuthor()));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────
    // 🔹 답변 생성
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/api/questions/{qId}/answers")
    public ResponseEntity<Answer> createAnswer(
            @PathVariable("qId") String questionId,
            @RequestBody Answer payload,
            HttpSession session) {

        Question q = questionRepository.findById(questionId).orElse(null);
        if (q == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");

        if (!StringUtils.hasText(userId))
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

        Answer a = new Answer();
        a.setQuestionId(questionId);
        a.setContent(payload.getContent() == null ? "" : payload.getContent());
        a.setCreatedAt(new Date());
        a.setAuthor(userId);

        try {
            a.getClass().getMethod("setAuthorRole", String.class).invoke(a, role);
        } catch (Exception ignore) {}

        Answer saved = answerRepository.save(a);

        // 알림 전송 (FCM)
        try {
            sendFcmForAnswer(saved, q, role);
        } catch (Exception ignore) {}

        saved.setTeacherName(resolveTeacherName(saved.getAuthor()));
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    // ─────────────────────────────────────────────────────────────
    // 🔹 답변 수정
    // ─────────────────────────────────────────────────────────────
    @PutMapping("/api/answers/{id}")
    public ResponseEntity<Answer> updateAnswer(
            @PathVariable String id,
            @RequestBody Answer answer) {

        Optional<Answer> opt = answerRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Answer existing = opt.get();
        existing.setContent(answer.getContent());
        Answer updated = answerRepository.save(existing);

        updated.setTeacherName(resolveTeacherName(updated.getAuthor()));
        return ResponseEntity.ok(updated);
    }

    // ─────────────────────────────────────────────────────────────
    // 🔹 답변 삭제 (Soft delete)
    // ─────────────────────────────────────────────────────────────
    @DeleteMapping("/api/answers/{id}")
    public ResponseEntity<Void> deleteAnswer(
            @PathVariable String id,
            HttpSession session) {

        String role = (String) session.getAttribute("role");
        if (!"teacher".equalsIgnoreCase(role))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Optional<Answer> opt = answerRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.noContent().build();

        Answer answer = opt.get();
        answer.setDeleted(true);
        answerRepository.save(answer);

        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────
    // 🔹 단일 답변 조회
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/api/answers/{id}")
    public ResponseEntity<Answer> getAnswer(@PathVariable String id) {
        Optional<Answer> opt = answerRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Answer a = opt.get();
        a.setTeacherName(resolveTeacherName(a.getAuthor()));
        return ResponseEntity.ok(a);
    }

    // ─────────────────────────────────────────────────────────────
    // 🔥 기존: 전체 최신 답변 n개 (사용 안함)
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/api/answers/recent")
    public List<Answer> getRecentAnswers(
            @RequestParam(defaultValue = "2") int count
    ) {
        Pageable page = PageRequest.of(0, count);
        List<Answer> list = answerRepository.findRecentActiveAnswers(page);

        for (Answer a : list) {
            a.setTeacherName(resolveTeacherName(a.getAuthor()));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────
    // 🔥🔥 NEW: 로그인한 학생/부모가 받은 ‘내 방’ 최신 답변
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/api/my/recent-answers")
    public List<Answer> getMyRecentAnswers(
            @RequestParam(defaultValue = "2") int count,
            HttpSession session
    ) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");

        if (role == null || userId == null) return List.of();

        List<Question> myRooms = new ArrayList<>();

        // 학생
        if ("student".equalsIgnoreCase(role)) {
            Student s = studentRepository.findByStudentId(userId);
            if (s != null && s.getAcademyNumbers() != null) {
                for (Integer ac : s.getAcademyNumbers()) {
                    myRooms.addAll(
                            questionRepository.findRoomByAcademyAndStudent(ac, userId)
                    );
                }
            }
        }

        // 부모
        else if ("parent".equalsIgnoreCase(role)) {
            Parent p = parentRepository.findByParentsId(userId);
            if (p != null) {
                List<Student> children = studentRepository.findByParentsNumber(p.getParentsNumber());
                if (children != null) {
                    for (Student child : children) {
                        if (child.getAcademyNumbers() == null) continue;
                        for (Integer ac : child.getAcademyNumbers()) {
                            myRooms.addAll(
                                    questionRepository.findRoomByAcademyAndParent(ac, userId)
                            );
                        }
                    }
                }
            }
        }

        if (myRooms.isEmpty()) return List.of();

        List<String> qIds = new ArrayList<>();
        for (Question q : myRooms) {
            if (q.getId() != null) qIds.add(q.getId());
        }

        if (qIds.isEmpty()) return List.of();

        List<Answer> all =
                answerRepository.findByQuestionIdInAndDeletedFalseOrderByCreatedAtDesc(qIds);

        int limit = Math.min(count, all.size());
        List<Answer> result = all.subList(0, limit);

        for (Answer a : result) {
            a.setTeacherName(resolveTeacherName(a.getAuthor()));
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // 🔹 공통 유틸
    // ─────────────────────────────────────────────────────────────
    private String resolveTeacherName(String teacherId) {
        if (!StringUtils.hasText(teacherId)) return "";
        Teacher t = teacherRepository.findByTeacherId(teacherId);
        return (t != null && StringUtils.hasText(t.getTeacherName()))
                ? t.getTeacherName()
                : teacherId;
    }

    private void sendFcmForAnswer(Answer saved, Question q, String role) {
        Set<String> sent = new HashSet<>();

        if ("teacher".equalsIgnoreCase(role) || "director".equalsIgnoreCase(role)) {
            // parent 전용 방
            if (StringUtils.hasText(q.getRoomParentId())) {
                Parent p = parentRepository.findByParentsId(q.getRoomParentId());
                if (p != null && StringUtils.hasText(p.getFcmToken()) && sent.add(p.getFcmToken())) {
                    fcmService.sendMessageTo(p.getParentsId(), p.getFcmToken(),
                            "새 답변 알림", "선생님의 답변이 도착했습니다.");
                }
            }

            // student 전용 방
            if (StringUtils.hasText(q.getRoomStudentId())) {
                Student s = studentRepository.findByStudentId(q.getRoomStudentId());
                if (s != null && StringUtils.hasText(s.getFcmToken()) && sent.add(s.getFcmToken())) {
                    fcmService.sendMessageTo(s.getStudentId(), s.getFcmToken(),
                            "새 답변 알림", "선생님의 답변이 도착했습니다.");
                }

                List<Parent> parents = parentRepository.findByStudentId(q.getRoomStudentId());
                if (parents != null) {
                    for (Parent p : parents) {
                        if (StringUtils.hasText(p.getFcmToken()) && sent.add(p.getFcmToken())) {
                            fcmService.sendMessageTo(p.getParentsId(), p.getFcmToken(),
                                    "새 답변 알림", "자녀 질문에 답변이 도착했습니다.");
                        }
                    }
                }
            }

        } else {
            // 학생/학부모 → 같은 학원 선생님들
            List<Teacher> teachers = teacherRepository.findByAcademyNumber(q.getAcademyNumber());
            if (teachers != null) {
                for (Teacher t : teachers) {
                    if (StringUtils.hasText(t.getFcmToken()) && sent.add(t.getFcmToken())) {
                        fcmService.sendMessageTo(t.getTeacherId(), t.getFcmToken(),
                                "새 메시지 알림", "새 메시지가 도착했습니다.");
                    }
                }
            }
        }
    }
}
