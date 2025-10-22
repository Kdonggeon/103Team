package com.team103.repository;

import com.team103.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {
    List<Course> findByTeacherId(String teacherId);
    List<Course> findByStudentsContaining(String studentId);
    List<Course> findByRoomNumberAndAcademyNumber(Integer roomNumber, Integer academyNumber);
    Optional<Course> findByClassId(String classId);

    default Course getByClassIdOrNull(String classId) {
        return findByClassId(classId).orElse(null);
    }
}
