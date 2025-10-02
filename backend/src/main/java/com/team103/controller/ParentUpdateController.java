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

    @PutMapping("/{id}")
    public ResponseEntity<?> updateParent(@PathVariable String id,
                                          @RequestBody ParentUpdateRequest req,
                                          Authentication auth) {
        // 1) 인증 사용자 확인
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "UNAUTHENTICATED"));
        }

        // 2) 권한: 본인 또는(옵션) 원장/관리자
        boolean isSelf = id.equals(auth.getName());
        boolean isDirector = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DIRECTOR".equals(a.getAuthority()));
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!(isSelf || isDirector || isAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "NO_PERMISSION"));
        }

        // 3) 대상 조회
        Parent p = parentRepository.findByParentsId(id);
        if (p == null) return ResponseEntity.notFound().build();

        // 4) 부분 업데이트(널만 아닌 값 반영)
        if (req.getParentsName() != null)         p.setParentsName(req.getParentsName());
        if (req.getParentsPhoneNumber() != null)  p.setParentsPhoneNumber(req.getParentsPhoneNumber());

        parentRepository.save(p);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
