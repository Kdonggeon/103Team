package com.team103.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team103.model.Attendance;
import com.team103.model.Course;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;

@RestController
@RequestMapping("/api/teachers")
public class TeacherAttendanceController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

//    // 1. 교사의 수업 목록 조회
//    @GetMapping("/{teacherId}/classes")
//    public ResponseEntity<List<Course>> getClassesByTeacher(@PathVariable String teacherId) {
//        List<Course> courses = courseRepository.findByTeacherId(teacherId);
//        return ResponseEntity.ok(courses);
//    }
//
//    // 2. 수업별 출석 조회
//    @GetMapping("/classes/{classId}/attendance")
//    public ResponseEntity<List<Attendance>> getAttendanceByClass(@PathVariable String classId) {
//        List<Attendance> attendances = attendanceRepository.findByClassId(classId);
//        return ResponseEntity.ok(attendances);
//    }

}
