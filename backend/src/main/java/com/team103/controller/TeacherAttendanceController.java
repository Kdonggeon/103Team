package com.team103.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team103.model.Attendance;
import com.team103.model.Course;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;
@RestController
@RequestMapping("/api/teachers/classes")
@CrossOrigin(origins = "*")
public class TeacherAttendanceController {

    private final AttendanceRepository attendanceRepo;

    public TeacherAttendanceController(AttendanceRepository attendanceRepo) {
        this.attendanceRepo = attendanceRepo;
    }

    /** 수업별 출석 현황 조회 (date 없으면 오늘) */
    @GetMapping("/{classId}/attendance")
    public ResponseEntity<List<Attendance>> getAttendanceByClass(
            @PathVariable String classId,
            @RequestParam(required = false) String date
    ) {
        String ymd = (date == null || date.isBlank())
                ? java.time.LocalDate.now().toString()  // "yyyy-MM-dd"
                : date;
        return ResponseEntity.ok(attendanceRepo.findByClassIdAndDate(classId, ymd));
    }
}
