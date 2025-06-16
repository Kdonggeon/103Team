package com.team103.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.team103.model.Course;

public interface CourseRepository extends MongoRepository<Course, String> {

    List<Course> findByStudentsContaining(String studentId);

    @Query("{ 'Class_ID' : ?0 }") // ✅ 정확히 Class_ID 필드 기준으로 찾음
    Course findByClassId(String classId);
    
    List<Course> findByTeacherId(String teacherId);
    
}

