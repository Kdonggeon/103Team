package com.team103.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team103.model.Attendance;
import com.team103.repository.AttendanceRepository;

@RestController
@RequestMapping("/api/classes")
public class ClassAttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @GetMapping("/{classId}/attendance")
    public ResponseEntity<List<Attendance>> getAttendanceByClassId(@PathVariable String classId) {
        List<Attendance> list = attendanceRepository.findByClassId(classId);
        return ResponseEntity.ok(list);
    }
}
