package com.team103.repository;

import com.team103.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface CourseRepository extends MongoRepository<Course, String> {

    // 학생이 포함된 수업
    List<Course> findByStudentsContaining(String studentId);

    // ✅ 컨트롤러·서비스에서 null 허용 형태로 많이 사용 중
    Course findByClassId(String classId);

    // 교사 ID로 수업 목록
    @Query("{ 'Teacher_ID': ?0 }")
    List<Course> findByTeacherId(String teacherId);

    // 방/학원으로 조회 (선택)
    List<Course> findByRoomNumberAndAcademyNumber(Integer roomNumber, Integer academyNumber);
}
