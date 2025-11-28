package com.team103.controller;

import com.team103.model.AcademyRequest;
import com.team103.service.AcademyRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academy-requests")
@CrossOrigin(origins = "*")
public class AcademyRequestController {

    @Autowired
    private AcademyRequestService service;

    public static class CreateRequest {
        public Integer academyNumber;
        public String requesterId;
        public String requesterRole;
        public String memo;
    }

    public static class ProcessRequest {
        public String processedBy;
        public String memo;
    }

    @PostMapping
    public ResponseEntity<AcademyRequest> create(@RequestBody CreateRequest body) {
        AcademyRequest req = new AcademyRequest();
        req.setAcademyNumber(body.academyNumber);
        req.setRequesterId(body.requesterId);
        req.setRequesterRole(body.requesterRole);
        req.setMemo(body.memo);
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping
    public ResponseEntity<List<AcademyRequest>> list(
            @RequestParam(value = "scope", defaultValue = "mine") String scope,
            @RequestParam(value = "requesterId", required = false) String requesterId,
            @RequestParam(value = "requesterRole", required = false) String requesterRole,
            @RequestParam(value = "academyNumber", required = false) Integer academyNumber,
            @RequestParam(value = "status", required = false) String status
    ) {
        if ("director".equalsIgnoreCase(scope)) {
            return ResponseEntity.ok(service.listByAcademy(academyNumber, status));
        }
        return ResponseEntity.ok(service.listMine(requesterId, requesterRole));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AcademyRequest> approve(
            @PathVariable String id,
            @RequestBody(required = false) ProcessRequest body
    ) {
        String by = body != null ? body.processedBy : null;
        String memo = body != null ? body.memo : null;
        return ResponseEntity.ok(service.approve(id, by, memo));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AcademyRequest> reject(
            @PathVariable String id,
            @RequestBody(required = false) ProcessRequest body
    ) {
        String by = body != null ? body.processedBy : null;
        String memo = body != null ? body.memo : null;
        return ResponseEntity.ok(service.reject(id, by, memo));
    }
}
