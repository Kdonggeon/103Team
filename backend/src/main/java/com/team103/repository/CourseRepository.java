package com.team103.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.team103.model.Course;

public interface CourseRepository extends MongoRepository<Course, String> {
    List<Course> findByStudentsContaining(String studentId);
    
    Course findByClassId(String classId);

    
    
}

