package com.team103.controller;

import com.team103.dto.SeatAssignRequest;
import com.team103.dto.SeatBoardResponse;
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

    /** 좌석 배정 (교사 수동/QR 공통)
     *  - 첫 좌석 배정이면 해당 학생의 출석 상태를 '출석'으로 마킹
     *  - seatNumber(정수) 또는 seatLabel(문자열) 모두 허용
     */
    @PostMapping("/{classId}/seats/assign")
    public ResponseEntity<Void> assignSeat(
            @PathVariable String classId,
            @RequestParam(required = false) String date, // 없으면 오늘
            @RequestBody SeatAssignRequest req
    ) {
        // 1) seatNumber 우선
        String seatLabel = (req.getSeatNumber() > 0)
                ? String.valueOf(req.getSeatNumber())
                : null;

        // 2) (선택) DTO에 seatLabel 게터가 있는 경우 사용(벡터 좌석이 문자 라벨일 때)
        if (seatLabel == null) {
            try {
                var m = req.getClass().getMethod("getSeatLabel");
                Object v = m.invoke(req);
                if (v != null && !String.valueOf(v).trim().isEmpty()) {
                    seatLabel = String.valueOf(v).trim();
                }
            } catch (ReflectiveOperationException ignore) {
                // 현재 DTO에 seatLabel 없으면 무시
            }
        }

        if (seatLabel == null || seatLabel.isBlank()) {
            throw new IllegalArgumentException("seatLabel/seatNumber required");
        }

        seatBoardService.assignSeat(classId, date, seatLabel, req.getStudentId()); // 내부에서 '출석' 처리까지
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

    /** (선택) 이동/휴식 상태로 바꾸기: 좌석 비우고 상태만 기록 */
    @PostMapping("/{classId}/seats/move")
    public ResponseEntity<Void> markMoveOrBreak(
            @PathVariable String classId,
            @RequestParam(required = false) String date,
            @RequestParam String studentId,
            @RequestParam(defaultValue = "이동") String status // "이동" | "휴식" | "대기" 등
    ) {
        seatBoardService.moveOrBreak(classId, date, studentId, status);
        return ResponseEntity.noContent().build();
    }
}
