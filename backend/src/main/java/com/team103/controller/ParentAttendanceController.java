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
@RequestMapping("/api/parents")
public class ParentAttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private CourseRepository courseRepository;

    @GetMapping("/{studentId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceForChild(@PathVariable String studentId) {
        List<Attendance> attendances = attendanceRepository.findByStudentInAttendanceList(studentId);
        List<AttendanceResponse> result = new ArrayList<>();

        for (Attendance att : attendances) {
            Course course = courseRepository.findByClassId(att.getClassId());
            String className = (course != null) ? course.getClassName() : "수업명 없음";

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
