package com.team103.controller;

import com.team103.dto.SeatBoardResponse;
import com.team103.dto.SeatAssignRequest;
import com.team103.service.SeatBoardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teachers/classes")
@CrossOrigin(origins = "*")
public class SeatBoardController {

    private final SeatBoardService seatBoardService;

    public SeatBoardController(SeatBoardService seatBoardService) {
        this.seatBoardService = seatBoardService;
    }

    /** 좌석판 조회 */
    @GetMapping("/{classId}/seat-board")
    public ResponseEntity<SeatBoardResponse> getSeatBoard(
            @PathVariable String classId,
            @RequestParam(required = false) String date
    ) {
        return ResponseEntity.ok(seatBoardService.getSeatBoard(classId, date));
    }

    /** 좌석 배정 (교사 수동/QR 공통) */
    @PostMapping("/{classId}/seats/assign")
    public ResponseEntity<Void> assignSeat(
            @PathVariable String classId,
            @RequestParam(required = false) String date, // 없으면 오늘
            @RequestBody SeatAssignRequest req
    ) {
        // 보통 seatNumber를 문자열 label로 사용
        String seatLabel = String.valueOf(req.getSeatNumber());
        seatBoardService.assignSeat(classId, date, seatLabel, req.getStudentId());
        return ResponseEntity.noContent().build();
    }

    /** 좌석 배정 해제 */
    @DeleteMapping("/{classId}/seats/{seatLabel}")
    public ResponseEntity<Void> unassignSeat(
            @PathVariable String classId,
            @PathVariable String seatLabel,
            @RequestParam(required = false) String date
    ) {
        seatBoardService.unassignSeat(classId, date, seatLabel);
        return ResponseEntity.noContent().build();
    }
}
