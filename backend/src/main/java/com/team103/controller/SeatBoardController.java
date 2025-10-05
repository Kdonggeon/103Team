package com.team103.controller;

import com.team103.dto.SeatBoardResponse;
import com.team103.service.SeatBoardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teachers/classes")
public class SeatBoardController {

    private final SeatBoardService seatBoardService;

    public SeatBoardController(SeatBoardService seatBoardService) {
        this.seatBoardService = seatBoardService;
    }

    @GetMapping("/{classId}/seatboard")
    public SeatBoardResponse seatboard(@PathVariable String classId,
                                       @RequestParam String date) {
        return seatBoardService.getSeatBoard(classId, date);
    }
}
