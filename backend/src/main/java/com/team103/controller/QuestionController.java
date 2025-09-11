package com.team103.controller;

import com.team103.model.Academy;
import com.team103.model.Answer;
import com.team103.model.Parent;
import com.team103.model.Question;
import com.team103.model.QuestionReadState;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.AcademyRepository;
import com.team103.repository.AnswerRepository;
import com.team103.repository.ParentRepository;
import com.team103.repository.QuestionReadStateRepository;
import com.team103.repository.QuestionRepository;
import com.team103.repository.StudentRepository;
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
    @Autowired private StudentRepository studentRepository;
    @Autowired private ParentRepository parentRepository;

    // 목록 (학원별 또는 전체)
    @GetMapping
    public List<Question> getQuestions(@RequestParam(value = "academyNumber", required = false) Integer academyNumber) {
        List<Question> list = (academyNumber != null)
                ? questionRepository.findByAcademyNumber(academyNumber)
                : questionRepository.findAll();
        for (Question q : list) populateExtras(q);
        return list;
    }

    // ✅ 학생별 1:1 방 보장: studentId 없으면 세션(role)로 자동 추론
    @GetMapping("/room")
    public ResponseEntity<Question> getOrCreateRoom(@RequestParam("academyNumber") int academyNumber,
                                                    @RequestParam(value = "studentId", required = false) String studentId,
                                                    HttpSession session) {
        String role = (String) session.getAttribute("role");       // "student" | "parent" | "teacher" | ...
        String userId = (String) session.getAttribute("username"); // LoginController 기준
        if (role == null || userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // --- studentId 자동 보정 ---
        if (studentId == null || studentId.trim().isEmpty()) {
            if ("student".equalsIgnoreCase(role)) {
                studentId = userId; // 학생은 본인
            } else if ("parent".equalsIgnoreCase(role)) {
                // 학부모는 자녀 목록 중 1명 선택(가능하면 해당 학원 소속 우선)
                Parent p = parentRepository.findByParentsId(userId);
                if (p != null && p.getStudentIds() != null && !p.getStudentIds().isEmpty()) {
                    String pick = null;
                    try {
                        for (String sid : p.getStudentIds()) {
                            Student s = studentRepository.findByStudentId(sid);
                            if (s != null && s.getAcademyNumbers() != null && s.getAcademyNumbers().contains(academyNumber)) {
                                pick = sid; break;
                            }
                        }
                    } catch (Exception ignore) {}
                    if (pick == null) pick = p.getStudentIds().get(0);
                    studentId = pick;
                }
            }
            // 교사는 여전히 null일 수 있음 → 아래에서 400 처리
        }

        if (studentId == null || studentId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        studentId = studentId.trim();

        // 1) 기존 방 찾기: 같은 학원 + 같은 학생(roomStudentId)
        List<Question> candidates = questionRepository.findByAcademyNumber(academyNumber);
        Question room = null;
        for (Question q : candidates) {
            if (q != null && studentId.equals(q.getRoomStudentId())) {
                room = q;
                break;
            }
        }

        // 2) 없으면 생성
        if (room == null) {
            room = new Question();
            room.setContent("");
            room.setAcademyNumber(academyNumber);
            room.setRoomStudentId(studentId);     // ★ 핵심: 학생별 방 식별자

            // 제목: "학생 {이름} 채팅방"
            String titleName = studentId;
            try {
                Student s = studentRepository.findByStudentId(studentId);
                if (s != null && s.getStudentName() != null && !s.getStudentName().isEmpty()) {
                    titleName = s.getStudentName();
                }
            } catch (Exception ignore) {}
            room.setTitle("학생 " + titleName + " 채팅방");

            // 방의 작성자(author)는 학생으로 고정(알림 분기 일관성)
            room.setAuthor(studentId);
            room.setAuthorRole("student");
            room.setCreatedAt(new Date());

            room = questionRepository.save(room);
        }

        // 3) 부가정보 + 미확인 답변 계산 후 반환
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
