package com.team103.controller;

import com.team103.model.Academy;
import com.team103.model.Answer;
import com.team103.model.Question;
import com.team103.model.QuestionReadState;
import com.team103.model.Teacher;
import com.team103.repository.AcademyRepository;
import com.team103.repository.AnswerRepository;
import com.team103.repository.QuestionReadStateRepository;
import com.team103.repository.QuestionRepository;
import com.team103.repository.TeacherRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    @Autowired private QuestionRepository questionRepository;
    @Autowired private AnswerRepository answerRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private AcademyRepository academyRepository;
    @Autowired private QuestionReadStateRepository readRepo;

    // 목록 (학원별 또는 전체)
    @GetMapping
    public List<Question> getQuestions(@RequestParam(value = "academyNumber", required = false) Integer academyNumber) {
        List<Question> list = (academyNumber != null)
                ? questionRepository.findByAcademyNumber(academyNumber)
                : questionRepository.findAll();
        for (Question q : list) populateExtras(q);
        return list;
    }

    // ✅ 학원별 채팅방(room) 보장: 있으면 반환, 없으면 생성 후 반환
    @GetMapping("/room")
    public ResponseEntity<Question> getOrCreateRoom(@RequestParam("academyNumber") int academyNumber,
                                                    HttpSession session) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");
        if (role == null || userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 1) 기존 room 찾기(타이틀 규칙 기반: "학원 {번호} 채팅방")
        String expectTitle = "학원 " + academyNumber + " 채팅방";
        List<Question> candidates = questionRepository.findByAcademyNumber(academyNumber);
        Question room = null;
        for (Question q : candidates) {
            String t = q.getTitle();
            if (t != null && t.trim().equals(expectTitle)) {
                room = q;
                break;
            }
        }

        // 2) 없으면 생성
        if (room == null) {
            room = new Question();
            room.setTitle(expectTitle);
            room.setContent("");
            room.setAcademyNumber(academyNumber);
            room.setAuthor(userId);
            room.setAuthorRole(role);
            room.setCreatedAt(new Date());
            room = questionRepository.save(room);
        }

        // 3) 부가정보 + 미확인 답변 계산 후 반환  ← (오류 원인이던 3줄을 여기로 이동)
        populateExtras(room);
        computeUnreadForUser(room, userId);
        return ResponseEntity.ok(room);
    }

    // 질문 생성
    @PostMapping
    public ResponseEntity<?> createQuestion(@RequestBody Question question,
                                            HttpSession session) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");

        if (userId == null || role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 후 이용해주세요.");
        }
        if (!(role.equalsIgnoreCase("student") || role.equalsIgnoreCase("parent"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("학생 또는 학부모만 질문을 생성할 수 있습니다.");
        }

        // ✅ 제목만 필수
        if (question.getTitle() == null || question.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("제목을 입력하세요.");
        }

        // 본문 미입력 허용
        if (question.getContent() == null) {
            question.setContent("");
        }

        question.setAuthor(userId);
        question.setAuthorRole(role);
        question.setCreatedAt(new Date()); // 생성 시각 보장

        // academyNumber는 클라이언트에서 넣어주는 값 사용(없으면 0 등)
        Question saved = questionRepository.save(question);
        return ResponseEntity.ok(saved);
    }

    // 질문 수정
    @PutMapping("/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable String id,
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

    // 단건
    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable String id) {
        return questionRepository.findById(id)
                .map(q -> { populateExtras(q); return ResponseEntity.ok(q); })
                .orElse(ResponseEntity.notFound().build());
    }

    // 부가정보(학원명, 교사이름들) 채우기
    private void populateExtras(Question q) {
        if (q == null) return;

        // 학원명
        try {
            Academy ac = academyRepository.findByNumber(q.getAcademyNumber());
            if (ac != null && ac.getName() != null) {
                q.setAcademyName(ac.getName());
            }
        } catch (Exception ignore) {}

        // 교사들 이름(등장 순서 유지, 중복 제거)
        try {
            List<Answer> answers = answerRepository.findActiveByQuestionId(q.getId());
            answers.sort(Comparator.comparing(Answer::getCreatedAt,
                    Comparator.nullsFirst(Comparator.naturalOrder())));

            LinkedHashSet<String> teacherIds = new LinkedHashSet<>();
            for (Answer a : answers) {
                if (a != null && a.getAuthor() != null && !a.getAuthor().isEmpty()) {
                    teacherIds.add(a.getAuthor()); // author = 교사ID
                }
            }

            List<String> names = new ArrayList<>();
            for (String tid : teacherIds) {
                Teacher t = teacherRepository.findByTeacherId(tid);
                names.add((t != null && t.getTeacherName() != null && !t.getTeacherName().isEmpty())
                        ? t.getTeacherName()
                        : tid);
            }
            q.setTeacherNames(names);
        } catch (Exception ignore) {}
    }

    // 방 내려줄 때(또는 목록 내려줄 때) 읽음 기준으로 미확인 계산
    private void computeUnreadForUser(Question q, String userId){
        if (q == null || userId == null) return;

        Optional<QuestionReadState> rsOpt = readRepo.findByQuestionIdAndUserId(q.getId(), userId);
        Date lastRead = rsOpt.map(QuestionReadState::getLastReadAt).orElse(null);

        List<Answer> answers = answerRepository.findActiveByQuestionId(q.getId());
        LinkedHashSet<String> teacherSet = new LinkedHashSet<>();
        int cnt = 0;

        for (Answer a : answers) {
            if (lastRead == null || (a.getCreatedAt()!=null && a.getCreatedAt().after(lastRead))) {
                cnt++;
                teacherSet.add(a.getAuthor()); // author=교사ID
            }
        }
        List<String> names = new ArrayList<>();
        for (String tid : teacherSet) {
            Teacher t = teacherRepository.findByTeacherId(tid);
            names.add((t!=null && t.getTeacherName()!=null && !t.getTeacherName().isEmpty()) ? t.getTeacherName() : tid);
        }
        q.setUnreadCount(cnt);
        q.setRecentResponderNames(names);
    }

    // 읽음 표시 API
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable String id, HttpSession session){
        String userId = (String) session.getAttribute("username");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        QuestionReadState rs = readRepo.findByQuestionIdAndUserId(id, userId)
                .orElseGet(() -> {
                    QuestionReadState n = new QuestionReadState();
                    n.setQuestionId(id);
                    n.setUserId(userId);
                    return n;
                });
        rs.setLastReadAt(new Date());
        readRepo.save(rs);
        return ResponseEntity.noContent().build();
    }
}
