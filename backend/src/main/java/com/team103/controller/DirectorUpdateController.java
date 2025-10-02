package com.team103.controller;

import com.team103.dto.DirectorUpdateRequest;
import com.team103.model.Director;
import com.team103.repository.DirectorRepository;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/directors")
public class DirectorUpdateController {

    private final DirectorRepository directorRepository;

    public DirectorUpdateController(DirectorRepository directorRepository) {
        this.directorRepository = directorRepository;
    }

    // PUT /api/directors/{id}  → {id}는 Director.username
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDirector(@PathVariable String id,
                                            @RequestBody DirectorUpdateRequest request,
                                            Authentication auth) {
        // 1) 인증 사용자 확인
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "UNAUTHENTICATED"));
        }

        // 2) 권한: 본인 또는(옵션) 관리자 허용
        boolean isSelf = id.equals(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!(isSelf || isAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "NO_PERMISSION"));
        }

        // 3) 대상 조회
        Director d = directorRepository.findByUsername(id);
        if (d == null) return ResponseEntity.notFound().build();

        // 4) 부분 업데이트 (null은 무시)
        if (request.getDirectorName() != null) {
            d.setName(request.getDirectorName());
        }
        if (request.getDirectorPhoneNumber() != null) {
            d.setPhone(request.getDirectorPhoneNumber());
        }
        if (request.getAcademyNumbers() != null) {
            // 비우고 싶으면 빈 배열 []을 보내세요. null은 "미변경"으로 처리됩니다.
            d.setAcademyNumbers(request.getAcademyNumbers());
        }

        directorRepository.save(d);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
