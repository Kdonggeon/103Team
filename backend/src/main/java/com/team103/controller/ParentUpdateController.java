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
    }
}
