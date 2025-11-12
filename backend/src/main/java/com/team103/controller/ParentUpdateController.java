package com.team103.controller;

import com.team103.dto.ParentUpdateRequest;
import com.team103.model.Parent;
import com.team103.repository.ParentRepository;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/parents")
public class ParentUpdateController {

    private final ParentRepository parentRepository;

<<<<<<< HEAD
    public ParentUpdateController(ParentRepository parentRepository) {
        this.parentRepository = parentRepository;
    }

    /** 1) 보호자 기본정보 수정 (아이디 변경 아님) */
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateParent(@PathVariable String id,
                                          @RequestBody ParentUpdateRequest req,
                                          Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "UNAUTHENTICATED"));
        }

        Parent p = parentRepository.findByParentsId(id);
        if (p == null) return ResponseEntity.notFound().build();

        if (req.getParentsName() != null)        p.setParentsName(req.getParentsName());
        if (req.getParentsPhoneNumber() != null) p.setParentsPhoneNumber(req.getParentsPhoneNumber());

        parentRepository.save(p);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /** 2) 보호자 아이디 변경: PATCH /api/parents/{id}/change-id  { "newId": "..." } */
    @PatchMapping(value = "/{id}/change-id", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> changeId(@PathVariable String id,
                                      @RequestBody Map<String, String> body,
                                      Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "UNAUTHENTICATED"));
        }

        String newId = body == null ? null : body.getOrDefault("newId", body.get("parentId"));
        if (newId == null || newId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "newId is required"));
        }
        if (id.equals(newId)) {
            return ResponseEntity.ok(Map.of("status", "noop", "id", id));
        }
        if (parentRepository.existsByParentsId(newId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "DUPLICATE_ID", "newId", newId));
        }

        Parent p = parentRepository.findByParentsId(id);
        if (p == null) return ResponseEntity.notFound().build();

        // @Id 가 parentsId 가 아닌 구조(일반적)인 경우: 필드만 갱신
        p.setParentsId(newId);
        parentRepository.save(p);

        return ResponseEntity.ok(Map.of("status", "ok", "oldId", id, "newId", newId));
    }

    /** 3) 쿼리파람 방식도 지원: PUT /api/parents/{id}?newId=...  (프론트의 폴백용) */
    @PutMapping(value = "/{id}", params = "newId")
    public ResponseEntity<?> changeIdByQuery(@PathVariable String id,
                                             @RequestParam String newId,
                                             Authentication auth) {
        return changeId(id, Map.of("newId", newId), auth);
    }

    /** 4) POST /api/parents/rename  { "oldId": "...", "newId": "..." } (프론트의 폴백용) */
    @PostMapping(value = "/rename", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> rename(@RequestBody Map<String,String> body, Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "UNAUTHENTICATED"));
        }
        String oldId = body == null ? null : body.get("oldId");
        String newId = body == null ? null : body.get("newId");
        if (oldId == null || newId == null || oldId.isBlank() || newId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "oldId/newId required"));
        }
        return changeId(oldId, Map.of("newId", newId), auth);
=======
    @PutMapping("/{id}")
    public ResponseEntity<?> updateParent(
            @PathVariable String id,
            @RequestBody ParentUpdateRequest request) {

        try {
            // ✅ 1️⃣ 기존 문서 조회
            Parent parent = parentRepository.findByParentsId(id);
            if (parent == null) {
                return ResponseEntity.badRequest().body("❌ 해당 학부모를 찾을 수 없습니다: " + id);
            }

            // ✅ 2️⃣ 필요한 필드만 수정 (비밀번호, ID 등은 유지)
            boolean updated = false;

            if (request.getParentsName() != null && !request.getParentsName().isBlank()) {
                parent.setParentsName(request.getParentsName());
                updated = true;
            }
            if (request.getParentsPhoneNumber() != null && !request.getParentsPhoneNumber().isBlank()) {
                parent.setParentsPhoneNumber(request.getParentsPhoneNumber());
                updated = true;
            }

            if (!updated) {
                return ResponseEntity.badRequest().body("⚠️ 변경된 정보가 없습니다.");
            }

            // ✅ 3️⃣ 기존 문서 그대로 저장 (덮어쓰기 아님)
            Parent saved = parentRepository.save(parent);

            // ✅ 4️⃣ 수정된 전체 정보 그대로 반환
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("❌ 서버 오류: " + e.getMessage());
        }
>>>>>>> new2
    }
}
