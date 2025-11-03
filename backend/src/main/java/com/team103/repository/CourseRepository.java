package com.team103.repository;

import com.team103.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;   // 👈 꼭 import
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {

    List<Course> findByTeacherId(String teacherId);

    List<Course> findByStudentsContaining(String studentId);

    List<Course> findByRoomNumberAndAcademyNumber(Integer roomNumber, Integer academyNumber);

    /** ⚠️ Mongo의 실제 필드명은 Class_ID 이므로 @Query로 매핑 */
    @Query("{ 'Class_ID': ?0 }")
    Optional<Course> findByClassId(String classId);   // 👈 컨트롤러 기존 코드 그대로 동작

    /** 같은 방 번호의 반(코스) 목록 — 방 중복 예약 검사용 */
    List<Course> findByRoomNumber(Integer roomNumber);
    List<Course> findByRoomNumbersContaining(Integer roomNumber);
    /** 편의 메서드 */
    default Course getByClassIdOrNull(String classId) {
        return findByClassId(classId).orElse(null);
    }
}
