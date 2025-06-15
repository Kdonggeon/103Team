package com.team103.controller;

import com.team103.dto.AttendanceResponse;
import com.team103.model.Attendance;
import com.team103.model.AttendanceEntry;
import com.team103.model.Course;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/students")
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private CourseRepository courseRepository;

    @GetMapping("/{studentId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByStudent(@PathVariable String studentId) {
        List<Attendance> attendances = attendanceRepository.findByStudentInAttendanceList(studentId);
        List<AttendanceResponse> result = new ArrayList<>();

        for (Attendance att : attendances) {
            Optional<Course> courseOpt = courseRepository.findById(att.getClassId());
            String className = courseOpt.map(Course::getClassName).orElse("수업명 없음");

            for (AttendanceEntry entry : att.getAttendanceList()) {
                if (entry.getStudentId().equals(studentId)) {
                    result.add(new AttendanceResponse(className, att.getDate(), entry.getStatus()));
                    break;
                }
            }
        }

        return ResponseEntity.ok(result);
    }
}
