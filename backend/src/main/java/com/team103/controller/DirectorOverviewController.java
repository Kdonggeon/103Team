// src/main/java/com/team103/controller/DirectorOverviewController.java
package com.team103.controller;

import com.team103.dto.DirectorOverviewResponse;
import com.team103.service.DirectorSeatOverviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/director/overview")
@CrossOrigin(origins = "*")
public class DirectorOverviewController {

 private final DirectorSeatOverviewService svc;
 public DirectorOverviewController(DirectorSeatOverviewService svc) { this.svc = svc; }

 // ✅ /api/director/overview  (구 버전)
 // ✅ /api/director/overview/  (트레일링 슬래시)
 // ✅ /api/director/overview/rooms  (신 버전)
 @GetMapping({"", "/", "/rooms"})
 public ResponseEntity<DirectorOverviewResponse> getAcademyOverview(
         @RequestParam int academyNumber,
         @RequestParam(required = false) String date
 ) {
     return ResponseEntity.ok(svc.getAcademyOverview(academyNumber, date));
 }
}

