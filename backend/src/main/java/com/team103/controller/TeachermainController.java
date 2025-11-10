package com.team103.controller;

import com.team103.dto.SeatBoardResponse;                     // ← SeatBoardService가 리턴하는 DTO
import com.team103.dto.TeacherClassLite;                      // ← TeacherTodayService가 리턴하는 DTO
import com.team103.service.SeatBoardService;
import com.team103.service.TeacherTodayService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/teachermain")
@CrossOrigin(origins = "*")
public class TeachermainController {

    private final TeacherTodayService todaySvc;
    private final SeatBoardService seatBoardSvc;

    public TeachermainController(TeacherTodayService todaySvc, SeatBoardService seatBoardSvc) {
        this.todaySvc = todaySvc;
        this.seatBoardSvc = seatBoardSvc;
    }

    /** 프론트: GET /api/teachermain/teachers/{teacherId}/classes/today?date=yyyy-MM-dd */
    @GetMapping("/teachers/{teacherId}/classes/today")
    public ResponseEntity<List<TeacherClassLite>> getTodayClasses(
            @PathVariable String teacherId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        String ymd = (date == null) ? null : date.toString();
        List<TeacherClassLite> out = todaySvc.getTodayClasses(teacherId, ymd);
        return ResponseEntity.ok(out);
    }

    /** 프론트: GET /api/teachermain/seat-board/{classId}?date=yyyy-MM-dd */
    @GetMapping("/seat-board/{classId}")
    public ResponseEntity<SeatBoardResponse> getSeatBoard(
            @PathVariable String classId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        String ymd = (date == null) ? null : date.toString();
        SeatBoardResponse res = seatBoardSvc.getSeatBoard(classId, ymd);
        return ResponseEntity.ok(res);
    }
}
