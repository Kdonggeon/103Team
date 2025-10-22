package com.team103.controller;

import com.team103.model.Course;
import com.team103.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentAttendanceController {

    @Autowired
    private CourseRepository courseRepo;

    /** 내가 수강하는 수업 조회 */
    @GetMapping("/{studentId}/classes")
    public List<Course> getMyClasses(@PathVariable String studentId) {
        return courseRepo.findByStudentsContaining(studentId);
    }

    // (참고) 출석 기록 조회는 AttendanceController가 담당
}
