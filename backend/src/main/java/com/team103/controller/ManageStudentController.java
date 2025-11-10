package com.team103.controller;

import com.team103.dto.ClassIdNameDto;
import com.team103.model.Course;
import com.team103.repository.CourseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manage/students")
public class ManageStudentController {

    private final CourseRepository courseRepo;
    public ManageStudentController(CourseRepository courseRepo) { this.courseRepo = courseRepo; }

    /**
     * Student 전용 라이트 목록
     * 예: GET /api/manage/students/st001/classes?lite=true
     */
    @GetMapping(value = "/{studentId}/classes", params = "lite=true")
    public ResponseEntity<List<ClassIdNameDto>> getClassesLite(@PathVariable String studentId) {
        List<Course> courses = courseRepo.findByStudentsContaining(studentId);
        return ResponseEntity.ok(
            courses.stream()
                   .map(ClassIdNameDto::fromCourse)
                   .filter(dto -> dto.getId() != null && !dto.getId().isBlank())
                   .toList()
        );
    }
}
