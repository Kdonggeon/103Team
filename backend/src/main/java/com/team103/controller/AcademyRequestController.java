package com.team103.controller;

import com.team103.model.AcademyRequest;
import com.team103.service.AcademyRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/academy-requests")
@CrossOrigin(origins = "*")
public class AcademyRequestController {

    @Autowired
    private AcademyRequestService service;

    public static class CreateRequest {
        @NotNull(message = "academyNumber는 필수입니다.")
        @Min(value = 1, message = "academyNumber는 1 이상이어야 합니다.")
        public Integer academyNumber;
        public String requesterId;
        public String requesterRole;
        public String studentId; // parent가 자녀를 지정할 때 사용
        @Size(max = 200, message = "memo는 200자 이하로 입력하세요.")
        public String memo;
    }

    public static class ProcessRequest {
        public String processedBy;
        public String memo;
    }

    private Authentication requireAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return auth;
    }

    private Set<String> roles(Authentication auth) {
        return auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_PARENT','ROLE_TEACHER','STUDENT','PARENT','TEACHER')")
    public ResponseEntity<AcademyRequest> create(@Valid @RequestBody CreateRequest body) {
        Authentication auth = requireAuth();
        Set<String> r = roles(auth);
        String requesterRole = r.contains("TEACHER") ? "teacher" : r.contains("PARENT") ? "parent" : "student";

        AcademyRequest req = new AcademyRequest();
        req.setAcademyNumber(body.academyNumber);
        req.setRequesterId(auth.getName());
        req.setRequesterRole(requesterRole);
        req.setTargetStudentId(body.studentId);
        req.setMemo(body.memo);
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AcademyRequest>> list(
            @RequestParam(value = "scope", defaultValue = "mine") String scope,
            @RequestParam(value = "requesterId", required = false) String requesterId,
            @RequestParam(value = "requesterRole", required = false) String requesterRole,
            @RequestParam(value = "academyNumber", required = false) Integer academyNumber,
            @RequestParam(value = "status", required = false) String status
    ) {
        Authentication auth = requireAuth();
        Set<String> r = roles(auth);
        if ("director".equalsIgnoreCase(scope)) {
            if (!r.contains("DIRECTOR")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "원장만 조회할 수 있습니다.");
            }
            return ResponseEntity.ok(service.listByAcademy(academyNumber, status));
        }
        // mine: 요청자 본인 기준으로만 조회
        String role = r.contains("TEACHER") ? "teacher" : r.contains("PARENT") ? "parent" : r.contains("STUDENT") ? "student" : requesterRole;
        return ResponseEntity.ok(service.listMine(auth.getName(), role));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_DIRECTOR','DIRECTOR')")
    public ResponseEntity<AcademyRequest> approve(
            @PathVariable String id,
            @RequestBody(required = false) ProcessRequest body
    ) {
        Authentication auth = requireAuth();
        String by = auth.getName();
        String memo = body != null ? body.memo : null;
        return ResponseEntity.ok(service.approve(id, by, memo));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_DIRECTOR','DIRECTOR')")
    public ResponseEntity<AcademyRequest> reject(
            @PathVariable String id,
            @RequestBody(required = false) ProcessRequest body
    ) {
        Authentication auth = requireAuth();
        String by = auth.getName();
        String memo = body != null ? body.memo : null;
        return ResponseEntity.ok(service.reject(id, by, memo));
    }
}
