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
import com.team103.security.JwtUtil;

import jakarta.servlet.http.HttpSession;

import io.jsonwebtoken.Claims;
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
    @Autowired private JwtUtil jwtUtil; // ★ 추가

    // === JWT → 세션 보완 (기존 기능 유지, 세션 없을 때만 채움) ===
    private static final String BEARER = "Bearer ";

    @ModelAttribute
    public void ensureSessionFromJwt(
            @RequestHeader(value = "Authorization", required = false) String auth,
            HttpSession session) {
        Object u = session.getAttribute("username");
        Object r = session.getAttribute("role");
        if ((u == null || r == null) && auth != null && auth.startsWith(BEARER)) {
            try {
                String token = auth.substring(BEARER.length());
                Claims claims = jwtUtil.validateToken(token);
                String userId = claims.getSubject();
                Object role = claims.get("role");
                if (userId != null && session.getAttribute("username") == null) {
                    session.setAttribute("username", userId);
                }
                if (role != null && session.getAttribute("role") == null) {
                    session.setAttribute("role", String.valueOf(role));
                }
            } catch (Exception ignore) {
                // 토큰이 유효하지 않으면 기존 흐름 유지
            }
        }
    }

    // === 내부 유틸 ===
    private boolean isParentOwnsRoom(Question q, String parentId) {
        return q != null
                && q.getRoomParentId() != null
                && q.getRoomParentId().equals(parentId);
    }

    // ID 하나로 학생/학부모 방 자동 판별 후 조회/생성 (교사/원장 전용)
    @GetMapping("/room/by-id")
    public ResponseEntity<Question> getOrCreateRoomById(@RequestParam("academyNumber") int academyNumber,
                                                        @RequestParam("id") String targetId,
                                                        HttpSession session) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");
        if (role == null || userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!(role.equalsIgnoreCase("teacher") || role.equalsIgnoreCase("director"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (targetId == null || targetId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        targetId = targetId.trim();

        Student s0 = null;
        Parent  p0 = null;
        try { s0 = studentRepository.findByStudentId(targetId); } catch (Exception ignore) {}
        try { p0 = parentRepository.findByParentsId(targetId); } catch (Exception ignore) {}

        boolean isStudent = (s0 != null);
        boolean isParent  = (p0 != null);

        if (!isStudent && !isParent) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Question room = null;

        if (isStudent) {
            // 학생 방 조회: 해당 학원 목록에서 roomStudentId 매칭
            List<Question> candidates = questionRepository.findByAcademyNumber(academyNumber);
            for (Question q : candidates) {
                if (q != null && targetId.equals(q.getRoomStudentId())) {
                    room = q;
                    break;
                }
            }
            // 없으면 생성
            if (room == null) {
                room = new Question();
                room.setAcademyNumber(academyNumber);
                room.setRoomStudentId(targetId);
                room.setRoom(true); // ★ 중요: 방 플래그 세팅

                String titleName = (s0 != null && s0.getStudentName() != null && !s0.getStudentName().isEmpty())
                        ? s0.getStudentName() : targetId;
                room.setTitle("학생 " + titleName + " 채팅방");

                room.setAuthor(targetId);
                room.setAuthorRole("student");
                room.setCreatedAt(new Date());
                room = questionRepository.save(room);
            }
        } else {
            // 학부모 방 조회: 기존 메서드 사용
            room = questionRepository.findRoomByAcademyAndParent(academyNumber, targetId);
            // 없으면 생성
            if (room == null) {
                room = new Question();
                room.setAcademyNumber(academyNumber);
                room.setRoomParentId(targetId);
                room.setRoom(true); // ★ 중요: 방 플래그 세팅

                String titleName = (p0 != null && p0.getParentsName() != null && !p0.getParentsName().isEmpty())
                        ? p0.getParentsName() : targetId;
                room.setTitle("보호자 " + titleName + " 채팅방");

                room.setAuthor(targetId);
                room.setAuthorRole("parent");
                room.setCreatedAt(new Date());
                room = questionRepository.save(room);
            }
        }

        populateExtras(room);
        computeUnreadForUser(room, userId);
        return ResponseEntity.ok(room);
    }

    // 교사/원장 전용: 학부모 ID로 방 조회/생성
    @GetMapping("/room/parent/for-teacher")
    public ResponseEntity<Question> getOrCreateParentRoomForTeacher(@RequestParam("academyNumber") int academyNumber,
                                                                    @RequestParam("parentId") String parentId,
                                                                    HttpSession session) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");
        if (role == null || userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!(role.equalsIgnoreCase("teacher") || role.equalsIgnoreCase("director"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (parentId == null || parentId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        parentId = parentId.trim();

        // 1) 기존 방 조회
        Question room = questionRepository.findRoomByAcademyAndParent(academyNumber, parentId);

        // 2) 없으면 생성
        if (room == null) {
            room = new Question();
            room.setAcademyNumber(academyNumber);
            room.setRoomParentId(parentId);   // 학부모 기준 방 식별자
            room.setRoom(true); // ★ 중요: 방 플래그 세팅

            // 제목: "보호자 {이름} 채팅방"
            String titleName = parentId;
            try {
                Parent p = parentRepository.findByParentsId(parentId);
                if (p != null && p.getParentsName() != null && !p.getParentsName().isEmpty()) {
                    titleName = p.getParentsName();
                }
            } catch (Exception ignore) {}
            room.setTitle("보호자 " + titleName + " 채팅방");

            room.setAuthor(parentId);
            room.setAuthorRole("parent");
            room.setCreatedAt(new Date());
            room = questionRepository.save(room);
        }

        populateExtras(room);
        computeUnreadForUser(room, userId);
        return ResponseEntity.ok(room);
    }

    // 목록 (학원별 또는 전체)
    @GetMapping
    public List<Question> getQuestions(@RequestParam(value = "academyNumber", required = false) Integer academyNumber,
                                       HttpSession session) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");

        // 학부모: 본인 방만 반환 (academyNumber 미지정 시 빈 리스트)
        if ("parent".equalsIgnoreCase(role)) {
            List<Question> result = new ArrayList<>();
            if (academyNumber != null) {
                Question room = questionRepository.findRoomByAcademyAndParent(academyNumber, userId);
                if (room != null) {
                    populateExtras(room);
                    computeUnreadForUser(room, userId);
                    result.add(room);
                }
            }
            return result;
        }

        // 그 외(학생/교사/원장): 기존 로직 유지
        List<Question> list = (academyNumber != null)
                ? questionRepository.findByAcademyNumber(academyNumber)
                : questionRepository.findAll();
        for (Question q : list) {
            populateExtras(q);
            if (userId != null) computeUnreadForUser(q, userId);
        }
        return list;
    }

    // 학생별 1:1 방 (학생/교사/원장)
    @GetMapping("/room")
    public ResponseEntity<Question> getOrCreateRoom(@RequestParam("academyNumber") int academyNumber,
                                                    @RequestParam(value = "studentId", required = false) String studentId,
                                                    HttpSession session) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");
        if (role == null || userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if ("parent".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        if (studentId == null || studentId.trim().isEmpty()) {
            if ("student".equalsIgnoreCase(role)) {
                studentId = userId;
            }
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
            room.setAcademyNumber(academyNumber);
            room.setRoomStudentId(studentId);
            room.setRoom(true); // ★ 중요: 방 플래그 세팅

            String titleName = studentId;
            try {
                Student s = studentRepository.findByStudentId(studentId);
                if (s != null && s.getStudentName() != null && !s.getStudentName().isEmpty()) {
                    titleName = s.getStudentName();
                }
            } catch (Exception ignore) {}
            room.setTitle("학생 " + titleName + " 채팅방");

            room.setAuthor(studentId);
            room.setAuthorRole("student");
            room.setCreatedAt(new Date());

            room = questionRepository.save(room);
        }

        populateExtras(room);
        computeUnreadForUser(room, userId);
        return ResponseEntity.ok(room);
    }

    // 학부모 전용: (academyNumber, parentId=세션) 기준 방 조회/생성
    @RequestMapping(value = "/room/parent", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Question> getOrCreateParentRoom(@RequestParam("academyNumber") int academyNumber,
                                                          HttpSession session) {
        String role = (String) session.getAttribute("role");
        String parentId = (String) session.getAttribute("username");
        if (role == null || parentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!"parent".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Question room = questionRepository.findRoomByAcademyAndParent(academyNumber, parentId);

        if (room == null) {
            room = new Question();
            room.setAcademyNumber(academyNumber);
            room.setRoomParentId(parentId);
            room.setRoom(true); // ★ 중요: 방 플래그 세팅
            room.setTitle("QnA");
            room.setAuthor(parentId);
            room.setAuthorRole("parent");
            room.setCreatedAt(new Date());
            room = questionRepository.save(room);
        }

        populateExtras(room);
        computeUnreadForUser(room, parentId);
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

        if (question.getTitle() == null || question.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("제목을 입력하세요.");
        }

        if (question.getContent() == null) {
            question.setContent("");
        }

        question.setAuthor(userId);
        question.setAuthorRole(role);
        question.setCreatedAt(new Date());

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

    // 단건 (학부모는 본인 방만 접근 가능)
    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable String id, HttpSession session) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");

        Optional<Question> opt = questionRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Question q = opt.get();

        if ("parent".equalsIgnoreCase(role)) {
            if (!isParentOwnsRoom(q, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        populateExtras(q);
        if (userId != null) computeUnreadForUser(q, userId);
        return ResponseEntity.ok(q);
    }

    // 부가정보(학원명, 교사이름들) 채우기
    private void populateExtras(Question q) {
        if (q == null) return;

        try {
            Academy ac = academyRepository.findByNumber(q.getAcademyNumber());
            if (ac != null && ac.getName() != null) {
                q.setAcademyName(ac.getName());
            }
        } catch (Exception ignore) {}

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

    // 읽음 표시 API (학부모는 본인 방만)
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable String id, HttpSession session){
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Question> opt = questionRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Question q = opt.get();

        if ("parent".equalsIgnoreCase(role)) {
            if (!isParentOwnsRoom(q, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

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

    // === 알림 유틸 (실제 발송 라인은 프로젝트의 서비스로 연결해 사용) ===
    private String getRoomCounterpartId(Question room, String senderRole) {
        if (room.getRoomStudentId() != null && !room.getRoomStudentId().isEmpty()) {
            if (!"student".equalsIgnoreCase(senderRole)) {
                return room.getRoomStudentId();
            }
        }
        if (room.getRoomParentId() != null && !room.getRoomParentId().isEmpty()) {
            if (!"parent".equalsIgnoreCase(senderRole)) {
                return room.getRoomParentId();
            }
        }
        return null;
    }

    private void notifyQnaReply(Question room, String senderRole, String previewText) {
        String title = (room.getTitle() != null && !room.getTitle().isEmpty()) ? room.getTitle() : "QnA";
        String body  = (previewText == null || previewText.isEmpty()) ? "새 답변이 도착했습니다." : previewText;

        if ("teacher".equalsIgnoreCase(senderRole) || "director".equalsIgnoreCase(senderRole)) {
            String targetId = getRoomCounterpartId(room, senderRole);
            if (targetId == null) return;

            // 예시) notificationService.sendToUser(targetId, title, body, room.getId());
        } else {
            // 예시) notificationService.sendToTeacherTopic(room.getAcademyNumber(), title, body, room.getId());
        }
    }
}
