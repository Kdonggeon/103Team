// src/main/java/com/team103/repository/CourseRepository.java
package com.team103.repository;

import com.team103.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {

    List<Course> findByTeacherId(String teacherId);
    List<Course> findByStudentsContaining(String studentId);
    List<Course> findByRoomNumberAndAcademyNumber(Integer roomNumber, Integer academyNumber);

    /** Mongo의 실제 필드명이 Class_ID */
    @Query("{ 'Class_ID': ?0 }")
    Optional<Course> findByClassId(String classId);

    /** ✅ 학원번호로 코스 전체 조회 (원장 종합뷰용) */
    @Query("{ 'Academy_Number': ?0 }")
    List<Course> findByAcademyNumber(int academyNumber);

    List<Course> findByRoomNumber(Integer roomNumber);
    List<Course> findByRoomNumbersContaining(Integer roomNumber);

    default Course getByClassIdOrNull(String classId) {
        return findByClassId(classId).orElse(null);
    }
}
