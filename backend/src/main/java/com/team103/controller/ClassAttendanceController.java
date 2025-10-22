package com.team103.controller;

import com.team103.model.Attendance;
import com.team103.repository.AttendanceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClassAttendanceController {

    private final AttendanceRepository attendanceRepository;

    public ClassAttendanceController(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping("/{classId}/attendance")
    public ResponseEntity<List<Attendance>> getAttendanceByClassId(@PathVariable String classId) {
        List<Attendance> list = attendanceRepository.findByClassId(classId); // ✅ 레포 메서드와 일치
        return ResponseEntity.ok(list);
    }
}
