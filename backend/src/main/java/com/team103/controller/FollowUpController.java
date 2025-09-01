package com.team103.controller;

import com.team103.model.FollowUp;
import com.team103.repository.FollowUpRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class FollowUpController {

    private final FollowUpRepository followUpRepository;

    public FollowUpController(FollowUpRepository followUpRepository) {
        this.followUpRepository = followUpRepository;
    }

    // 목록
    @GetMapping("/questions/{qId}/followups")
    public List<FollowUp> list(@PathVariable("qId") String questionId) {
        List<FollowUp> list = followUpRepository.findByQuestionIdAndDeletedFalse(questionId);
        // createdAt 오름차순 정렬
        list.sort(Comparator.comparing(FollowUp::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    // 생성: 학생/학부모만
    @PostMapping("/questions/{qId}/followups")
    public ResponseEntity<?> create(@PathVariable("qId") String questionId,
                                    @RequestBody FollowUp fu,
                                    HttpSession session) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");
        if (userId == null || role == null) {
            return ResponseEntity.status(401).body("로그인 후 이용해주세요.");
        }
        if (!(role.equalsIgnoreCase("student") || role.equalsIgnoreCase("parent"))) {
            return ResponseEntity.status(403).body("학생 또는 학부모만 후속 질문을 작성할 수 있습니다.");
        }

        fu.setQuestionId(questionId);
        fu.setAuthor(userId);
        fu.setAuthorRole(role);
        fu.setCreatedAt(new Date());
        fu.setDeleted(false);

        FollowUp saved = followUpRepository.save(fu);
        return ResponseEntity.ok(saved);
    }

    // 수정: 작성자 본인만(학생/학부모)
    @PutMapping("/followups/{id}")
    public ResponseEntity<?> update(@PathVariable String id,
                                    @RequestBody FollowUp req,
                                    HttpSession session) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");
        if (userId == null || role == null) return ResponseEntity.status(401).build();
        if (!(role.equalsIgnoreCase("student") || role.equalsIgnoreCase("parent"))) return ResponseEntity.status(403).build();

        Optional<FollowUp> opt = followUpRepository.findById(id);
        if (!opt.isPresent()) return ResponseEntity.notFound().build();

        FollowUp fu = opt.get();
        if (!userId.equals(fu.getAuthor())) return ResponseEntity.status(403).body("작성자만 수정할 수 있습니다.");

        fu.setContent(req.getContent());
        FollowUp saved = followUpRepository.save(fu);
        return ResponseEntity.ok(saved);
    }

    // 삭제(소프트): 작성자 or 선생
    @DeleteMapping("/followups/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, HttpSession session) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("username");
        if (userId == null || role == null) return ResponseEntity.status(401).build();

        Optional<FollowUp> opt = followUpRepository.findById(id);
        if (!opt.isPresent()) return ResponseEntity.noContent().build();

        FollowUp fu = opt.get();
        boolean isAuthor = userId.equals(fu.getAuthor());
        boolean isTeacher = "teacher".equalsIgnoreCase(role);
        if (!(isAuthor || isTeacher)) return ResponseEntity.status(403).build();

        if (!fu.isDeleted()) {
            fu.setDeleted(true);
            followUpRepository.save(fu);
        }
        return ResponseEntity.noContent().build();
    }
}
