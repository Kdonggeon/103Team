package com.team103.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/students")
public class StudentAttendanceController {

    @Autowired private CourseRepository classRepo;
    @Autowired private AttendanceRepository attendanceRepo;

    // 1. 내가 수강하는 수업 조회
    @GetMapping("/{studentId}/classes")
    public List<Course> getMyClasses(@PathVariable String studentId) {
        return classRepo.findByStudentsContaining(studentId);
    }

    // 2. 내가 출석한 기록 확인
    @GetMapping("/{studentId}/attendance")
    public List<Attendance> getMyAttendance(
            @PathVariable String studentId,
            @RequestParam String classId) {
        return attendanceRepo.findByClassIdAndAttendedStudentsContaining(classId, studentId);
    }
}

